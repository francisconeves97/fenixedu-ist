package pt.ist.registration.process.handler;

import org.springframework.stereotype.Component;

import pt.tecnico.ulisboa.dsi.employer.Employer;
import pt.tecnico.ulisboa.dsi.employer.backoff.ExponentialBackoff;
import pt.tecnico.ulisboa.dsi.employer.workflow.SimpleWorkflow;

@Component
public class EmployerInitializer {

    private Employer employer;

    public EmployerInitializer() {
        this.employer = new Employer(new SimpleWorkflow(), new ExponentialBackoff(), 5);
    }

    public Employer getEmployer() {
        return employer;
    }

}
