/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST CMS Components.
 *
 * FenixEdu IST CMS Components is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST CMS Components is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST CMS Components.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.cmscomponents.ui.spring;

import static java.util.stream.Collectors.toList;
import static pt.ist.fenixframework.FenixFramework.atomic;

import javax.servlet.http.HttpServletRequest;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.AllAlumniGroup;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.WebAddress;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.spring.portal.SpringApplication;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.LocalizedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageListener;
import pt.ist.fenixedu.cmscomponents.domain.homepage.HomepageSite;

@SpringApplication(group = "logged", path = "personal-homepage", title = "application.homepage.admin")
@SpringFunctionality(app = HomepageAdminController.class, title = "application.homepage.admin")
@RequestMapping("/personal-homepage")
public class HomepageAdminController {

    @Autowired
    PagesAdminService service;

    @RequestMapping(method = RequestMethod.GET)
    public String options(Model model) {
        Person person = loggedPerson();
        Site homepage = person.getHomepage();

        model.addAttribute("homepage", homepage);
        model.addAttribute("person", person);

        if (homepage != null) {
            model.addAttribute("dynamicPages", service.dynamicPages(homepage).collect(toList()));
            model.addAttribute("isAlumi", new AllAlumniGroup().isMember(person.getUser()));
        }

        return "fenix-learning/homepageOptions";
    }

    @RequestMapping(value = "/content", method = RequestMethod.GET)
    public String content(Model model) {
        model.addAttribute("homepage", loggedPerson().getHomepage());
        return "fenix-learning/homepageContent";
    }

    @RequestMapping(value = "/options", method = RequestMethod.POST)
    public RedirectView editHomepageOptions(@RequestParam(required = false, defaultValue = "false") Boolean showPhoto,
            @RequestParam(required = false, defaultValue = "false") Boolean showCategory, @RequestParam(required = false,
                    defaultValue = "false") Boolean showResearchUnitHomepage, @RequestParam(required = false,
                    defaultValue = "false") Boolean showActiveStudentCurricularPlans, @RequestParam(required = false,
                    defaultValue = "false") Boolean published, @RequestParam(required = false) String researchUnitHomepage,
            @RequestParam(required = false) LocalizedString researchUnitName, @RequestParam(required = false,
                    defaultValue = "false") Boolean showUnit,
            @RequestParam(required = false, defaultValue = "false") Boolean showCurrentExecutionCourses, @RequestParam(
                    required = false, defaultValue = "false") Boolean showCurrentAttendingExecutionCourses, @RequestParam(
                    required = false, defaultValue = "false") Boolean showAlumniDegrees) {
        atomic(() -> {
            if (loggedPerson().getHomepage() == null) {
                HomepageListener.create(loggedPerson());
            } else {
                Site site = loggedPerson().getHomepage();
                HomepageSite homepage = site.getHomepageSite();
                homepage.setShowPhoto(showPhoto);
                homepage.setShowCategory(showCategory);
                homepage.setShowResearchUnitHomepage(showResearchUnitHomepage);
                homepage.setShowActiveStudentCurricularPlans(showActiveStudentCurricularPlans);
                homepage.setResearchUnitHomepage(researchUnitHomepage);
                homepage.setResearchUnitName(researchUnitName);
                homepage.getSite().setPublished(published);
                homepage.getSite().setCanViewGroup(Group.anyone());
                homepage.setShowUnit(showUnit);
                homepage.setShowCurrentExecutionCourses(showCurrentExecutionCourses);
                homepage.setShowCurrentAttendingExecutionCourses(showCurrentAttendingExecutionCourses);
                homepage.setShowAlumniDegrees(showAlumniDegrees);
    
                String url = homepage.getSite().getFullUrl();
                if (published) {
                    boolean foundAddress = false;
                    for (PartyContact contact : site.getOwner().getPartyContacts(WebAddress.class)) {
                        WebAddress address = (WebAddress) contact;
                        if (address.getUrl().equals(url)) {
                            address.setDefaultContact(true);
                            foundAddress = true;
                        } else {
                            address.setDefaultContact(false);
                        }
                    }
                    if (!foundAddress) {
                        WebAddress.createWebAddress(site.getOwner(), url, PartyContactType.INSTITUTIONAL, true);
                    }
                } else {
                    WebAddress address = site.getOwner().getDefaultWebAddress();
                    if (address != null && address.getUrl().equals(url)) {
                        address.setDefaultContact(false);
                    }
                }

            }
        });
        return new RedirectView("/personal-homepage", true);
    }

    @RequestMapping(value = "/activePages", method = RequestMethod.POST)
    public RedirectView editActivePages(HttpServletRequest request) {
        Site site = loggedPerson().getHomepage();
        if (site != null) {
            atomic(() -> service.dynamicPages(site).forEach(
                    page -> page.setPublished(request.getParameterMap().keySet().contains(page.getSlug()))));
        }
        return new RedirectView("/personal-homepage", true);
    }

    private Person loggedPerson() {
        AccessControl.check((obj) -> AccessControl.getPerson() != null);
        return AccessControl.getPerson();
    }
}
