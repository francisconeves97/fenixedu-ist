package pt.ist.registration.process.domain.beans;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;

import pt.ist.registration.process.domain.DeclarationTemplate;

public class DeclarationTemplateInputFormBean {

    private ExecutionYear executionYear;

    private Registration registration;

    private DeclarationTemplate declarationTemplate;

    public DeclarationTemplateInputFormBean() {
    }

    public DeclarationTemplate getDeclarationTemplate() {
        return declarationTemplate;
    }

    public void setDeclarationTemplate(DeclarationTemplate declarationTemplate) {
        this.declarationTemplate = declarationTemplate;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

}
