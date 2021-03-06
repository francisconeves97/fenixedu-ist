/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Integration.
 *
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.integration.ui.renderers.providers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Unit;

import pt.ist.fenixWebFramework.renderers.DataProvider;
import pt.ist.fenixWebFramework.renderers.components.converters.Converter;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.integration.domain.accessControl.PersistentGroupMembers;
import pt.ist.fenixedu.integration.ui.struts.action.research.researchUnit.PersistentGroupMembersBean;

public class PeopleForUnitGroups implements DataProvider {

    @Override
    public Converter getConverter() {
        return null;
    }

    @Override
    public Object provide(Object source, Object currentValue) {
        Unit unit;
        if (source instanceof Unit) {
            unit = (Unit) source;
        } else {
            unit = ((PersistentGroupMembersBean) source).getUnit();
        }
        return getPeopleForUnit(unit);
    }

    private Collection<Person> getPeopleForUnit(Unit unit) {
        Set<Person> people = new HashSet<Person>();
        people.addAll(Employee.getPossibleGroupMembers(unit));
        for (PersistentGroupMembers persistentGroupMembers : unit.getPersistentGroupsSet()) {
            for (Person person : persistentGroupMembers.getPersonsSet()) {
                if (!people.contains(person)) {
                    people.add(person);
                }
            }
        }
        return people;
    }
}
