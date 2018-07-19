package pt.ist.fenixedu.integration.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.fenixedu.cms.domain.Post;
import org.joda.time.DateTime;

public class FenixCourseAnnouncement {
    private String name;
    private String body;
    private String link;
    private String author = "";
    private String authorId = "";
    private String guid;
    private String publicationDate;

    public FenixCourseAnnouncement(Post post) {
        this.name = post.getName().json().toString();
        this.body = post.getBody().json().toString();
        this.link = post.getAddress();
        if (post.getCreatedBy().getProfile() != null && post.getCreatedBy().getProfile().getEmail() != null
                && post.getCreatedBy().getProfile().getPerson() != null) {
            this.author = post.getCreatedBy().getProfile().getEmail();
            this.authorId = post.getCreatedBy().getProfile().getPerson().getUsername();
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
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
