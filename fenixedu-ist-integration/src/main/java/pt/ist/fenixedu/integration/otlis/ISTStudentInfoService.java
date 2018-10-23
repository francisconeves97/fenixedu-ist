package pt.ist.fenixedu.integration.otlis;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.otlis.core.dto.StudentInfo;
import org.fenixedu.otlis.core.service.StudentInfoService;


public class ISTStudentInfoService implements StudentInfoService {

    public StudentInfo getStudentInfoByUsername(String username) {
        Person person = Person.findByUsername(username);

        if (person == null) {
            return null;
        }

        Student student = person.getStudent();
        String registrationDeclarationUrl = student.getLastRegistration().getRegistrationDeclarationFileSet().iterator().next().getDownloadSignedFileLink();

        return new StudentInfo(username, student.getName(), registrationDeclarationUrl);
    }
}
