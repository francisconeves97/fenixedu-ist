package org.fenixedu.ulisboa.otlis.service;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.otlis.core.dto.StudentInfo;
import org.fenixedu.otlis.core.service.StudentInfoService;

import java.util.Optional;

public class ISTStudentInfoService implements StudentInfoService {

    public Optional<StudentInfo> getStudentInfoByUsername(String username) {
        Person person = Person.findByUsername(username);

        if (person == null) {
            return Optional.empty();
        }

        Student student = person.getStudent();
        String registrationDeclarationUrl = student.getLastRegistration().getRegistrationDeclarationFileSet().iterator().next().getDownloadSignedFileLink();

        return Optional.of(new StudentInfo(username, student.getName(), registrationDeclarationUrl));
    }
}
