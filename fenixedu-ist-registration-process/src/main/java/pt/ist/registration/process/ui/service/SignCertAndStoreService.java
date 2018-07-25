package pt.ist.registration.process.ui.service;

import static org.fenixedu.bennu.RegistrationProcessConfiguration.RESOURCE_BUNDLE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.messaging.core.domain.Message;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.gson.JsonObject;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import pt.ist.drive.sdk.ClientFactory;
import pt.ist.drive.sdk.DriveClient;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;
import pt.ist.registration.process.handler.CandidacySignalHandler;
import pt.ist.registration.process.handler.EmployerInitializer;
import pt.tecnico.ulisboa.dsi.employer.exception.JobFailedException;
import pt.tecnico.ulisboa.dsi.employer.job.Job;

@Service
public class SignCertAndStoreService {

    private static final Logger logger = LoggerFactory.getLogger(SignCertAndStoreService.class);

    @Autowired
    private EmployerInitializer employerInitializer;

    private final Client client;
    private final String driveUrl;
    private final String appId;
    private final String refreshToken;
    private final String appUser;

    public SignCertAndStoreService() {
        this.client = ClientBuilder.newClient();
        this.client.register(MultiPartFeature.class);
        this.client.register(JsonBodyReaderWriter.class);
        driveUrl = RegistrationProcessConfiguration.getConfiguration().storeUrl();
        appId = RegistrationProcessConfiguration.getConfiguration().storeAppId();
        refreshToken = RegistrationProcessConfiguration.getConfiguration().storeAppRefreshToken();
        appUser = RegistrationProcessConfiguration.getConfiguration().storeAppUser();
    }

    public void sendDocumentToBeSignedWithJob(Registration registration, RegistrationDeclarationFile file, String queue)
            throws Error {

        employerInitializer.getEmployer()
                .offer(new SignerJob(registration, file, queue));
    }

    public void sendDocumentToBeSigned(String registrationExternalId, String queue, String title, String description,
            String filename, InputStream contentStream, String uniqueIdentifier) throws Error {
        logger.debug("Sending document to be signed with id {}", uniqueIdentifier);
        String compactJws = Jwts.builder()
                                .setSubject(RegistrationProcessConfiguration.getConfiguration().signerJwtUser())
                                .setExpiration(DateTime.now().plusHours(6).toDate())
                                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();

        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
            final StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", contentStream, filename, new MediaType("application", "pdf"));
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("queue", queue));
            formDataMultiPart.bodyPart(
                    new FormDataBodyPart("creator", "Sistema FenixEdu"));
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("title", title));
            formDataMultiPart.bodyPart(new FormDataBodyPart("description", description));
            formDataMultiPart.bodyPart(new FormDataBodyPart("externalIdentifier", uniqueIdentifier));
            formDataMultiPart.bodyPart(new FormDataBodyPart("signatureField", CandidacySignalHandler.SIGNATURE_FIELD));

            String nounce = Jwts.builder().setSubject(uniqueIdentifier)
                    .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();

            formDataMultiPart.bodyPart(new FormDataBodyPart("callbackUrl", CoreConfiguration.getConfiguration().applicationUrl()
                    + "/registration-process/registration-declarations/sign/" + registrationExternalId + "?nounce=" + nounce));
            String result = client.target(RegistrationProcessConfiguration.getConfiguration().signerUrl()).path("sign-requests")
                    .request().header("Authorization", "Bearer " + compactJws)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
            logger.debug(result);
        } catch (final IOException e) {
            throw new Error(e);
        } 
    }

    public void sendDocumentToBeCertified(String registrationExternalId, String filename, MultipartFile file,
            String uniqueIdentifier, boolean alreadyCertified) {

        try {
            sendDocumentToBeCertified(registrationExternalId, filename, file.getBytes(), uniqueIdentifier, alreadyCertified);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public void sendDocumentToBeCertified(String registrationExternalId, String filename, byte[] fileBytes,
            String uniqueIdentifier, boolean alreadyCertified) {
        String compactJws = Jwts.builder().setSubject(uniqueIdentifier)
                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();

        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
                InputStream fileStream = new ByteArrayInputStream(fileBytes)) {
            final StreamDataBodyPart streamDataBodyPart =
                    new StreamDataBodyPart("file", fileStream, filename, new MediaType("application", "pdf"));
            formDataMultiPart.bodyPart(streamDataBodyPart);
            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
            formDataMultiPart.bodyPart(new FormDataBodyPart("mimeType", "application/json"));
            formDataMultiPart.bodyPart(new FormDataBodyPart("identifier", uniqueIdentifier));
            formDataMultiPart.bodyPart(new FormDataBodyPart("alreadyCertified", Boolean.valueOf(alreadyCertified).toString()));

            String nounce = Jwts.builder().setSubject(uniqueIdentifier)
                    .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.certifierJwtSecret()).compact();
            formDataMultiPart.bodyPart(new FormDataBodyPart("callback", CoreConfiguration.getConfiguration().applicationUrl()
                    + "/registration-process/registration-declarations/cert/" + registrationExternalId + "?nounce=" + nounce));
            String result = client.target(RegistrationProcessConfiguration.getConfiguration().certifierUrl())
                    .path("api/documents/certify").request().header("Authorization", "Bearer " + compactJws)
                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
            logger.debug(result);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public void sendDocumentToBeStored(String username, String email, RegistrationDeclarationFile declarationFile, MultipartFile file) throws
            IOException {
        sendDocumentToBeStored(username, email, declarationFile, file.getBytes(), file.getContentType());
    }

    public void sendDocumentToBeStored(String username, String email, RegistrationDeclarationFile declarationFile,
            byte[] fileBytes, String fileContentType) throws IOException {
        DriveClient driveClient = ClientFactory.driveCLient(driveUrl, appId, appUser, refreshToken);
        String directory = uploadDirectoryFor(driveClient, username).get();
        InputStream fileStream = new ByteArrayInputStream(fileBytes);
        JsonObject result =
                driveClient.uploadWithInfo(directory, declarationFile.getFilename(), fileStream, fileContentType);
        logger.debug("Registration Declaration {} of student {} stored", declarationFile.getUniqueIdentifier(), username);
        logger.debug("Registration Declaration {} of student {} is being emailed.", declarationFile.getUniqueIdentifier(),
                username);
        final String downloadFileLink = result.get("downloadFileLink").getAsString();
        declarationFile.updateState(RegistrationDeclarationFileState.STORED, null, null, downloadFileLink);
        sendEmailNotification(email, declarationFile.getDisplayName(), downloadFileLink, declarationFile.getLocale());

    }

    @Atomic(mode = TxMode.WRITE)
    private void sendEmailNotification(String email, String displayName,  String link, Locale locale) {
        String title = BundleUtil.getString(RESOURCE_BUNDLE, locale,"registration.document.email.title", displayName);
        String body = BundleUtil.getString(RESOURCE_BUNDLE, locale, "registration.document.email.body", displayName, link);

        Message.fromSystem()
                .replyToSender()
                .singleBcc(email)
                .subject(title)
                .textBody(body)
                .send();
    }

    private Optional<String> uploadDirectoryFor(DriveClient driveClient, String username) {
        String directoryName = Joiner.on("/").join(username, RegistrationProcessConfiguration.getConfiguration().storeDirectoryName());
        List<String> slugParts = Splitter.on("/").splitToList(directoryName);
        JsonObject directory = driveClient.getDirectoryWithSlug(slugParts);
        return Optional.ofNullable(directory).map(o -> o.get("id").getAsString());
    }

    public class SignerJob extends Job {

        private Registration registration;

        private RegistrationDeclarationFile file;

        private String fileQueue;

        public SignerJob(Registration registration, RegistrationDeclarationFile file, String fileQueue) {
            super();
            this.registration = registration;
            this.file = file;
            this.fileQueue = fileQueue;
        }

        @Override
        public void start() {
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void execute() throws JobFailedException {
            try {
                String filename = file.getFilename();
                String title = file.getDisplayName();
                String externalIdentifier = file.getUniqueIdentifier();

                sendDocumentToBeSigned(registration.getExternalId(), fileQueue, title, title, filename,
                        file.getStream(), externalIdentifier);
            } catch (Throwable t) {
                t.printStackTrace();
                throw new JobFailedException();
            }
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void finish() {
            try {
                logger.debug("Registration Declaration {} of student {} was sent to signer", file.getUniqueIdentifier(),
                        registration.getNumber());
                file.updateState(RegistrationDeclarationFileState.PENDING);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        @Override
        @Atomic(mode = TxMode.READ)
        public void fail() {
            logger.debug("Registration Declaration {} of student {} SignerJob failed", file.getUniqueIdentifier(),
                    registration.getNumber());
        }

    }
}
