package pt.ist.fenixedu.integration.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.cms.domain.Post;

public class FenixCourseAnnouncement {
    private String name;
    private String body;
    private String link;
    private FenixAuthor author;
    private String guid;
    private String publicationDate;

    private class FenixAuthor {

        private String name;
        private String email;
        private String username;

        private FenixAuthor(String name, String email, String username) {
            this.name = name;
            this.email = email;
            this.username = username;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    public FenixCourseAnnouncement(Post post) {
        this.name = post.getName().json().toString();
        this.body = post.getBody().json().toString();
        this.link = post.getAddress();
        if (post.getCreatedBy().getProfile() != null && post.getCreatedBy().getProfile().getPerson() != null) {
            Person person = post.getCreatedBy().getProfile().getPerson();

            this.author = new FenixAuthor(person.getDisplayName(), post.getCreatedBy().getEmail(), person.getUsername());
        }
        this.guid = post.getAddress() + "#" + post.getExternalId();
        this.publicationDate = post.getCreationDate().toString("yyyy-MM-dd HH:mm:ss");
    }

    @JsonRawValue
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonRawValue
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public FenixAuthor getAuthor() {
        return author;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }
}
