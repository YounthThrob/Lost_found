package com.lostfound.directory;

import com.lostfound.operations.OperatorGuard;
import com.lostfound.shared.Envelope;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DirectoryController {

    private final DirectoryService service;
    private final OperatorGuard guard;

    public DirectoryController(DirectoryService service, OperatorGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    public record SchoolView(String id, String name, List<String> aliases) {
        static SchoolView from(School s) {
            return new SchoolView(s.id(), s.name(), s.aliases());
        }
    }

    public record CampusView(String id, String schoolId, String name, String status) {
        static CampusView from(Campus c) {
            return new CampusView(c.id(), c.schoolId(), c.name(), c.status());
        }
    }

    public record SchoolListData(List<SchoolView> items, DirectoryService.Pagination pagination) {
    }

    public record CampusListData(List<CampusView> items, DirectoryService.Pagination pagination) {
    }

    public record CampusCreate(String name) {
    }

    public record CampusReview(String decision) {
    }

    @GetMapping("/directory/schools")
    public Envelope.Response<SchoolListData> searchSchools(Authentication authentication,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        DirectoryService.SchoolListResult result = service.searchSchools(keyword, page, pageSize);
        List<SchoolView> views = result.items().stream().map(SchoolView::from).toList();
        return Envelope.ok(new SchoolListData(views, result.pagination()));
    }

    @GetMapping("/directory/schools/{schoolId}/campuses")
    public Envelope.Response<CampusListData> listCampuses(Authentication authentication,
                                                          @PathVariable String schoolId) {
        List<Campus> campuses = service.listCampuses(schoolId);
        List<CampusView> views = campuses.stream().map(CampusView::from).toList();
        DirectoryService.Pagination p = new DirectoryService.Pagination(1, views.size(), views.size(), false);
        return Envelope.ok(new CampusListData(views, p));
    }

    @PostMapping("/directory/schools/{schoolId}/campuses")
    public Envelope.Response<CampusView> requestCampus(Authentication authentication,
                                                       @PathVariable String schoolId,
                                                       @RequestBody CampusCreate request) {
        return Envelope.ok(CampusView.from(service.requestCampus(schoolId, request.name())));
    }

    @PostMapping("/admin/campuses/{campusId}/review")
    public Envelope.Response<CampusView> reviewCampus(Authentication authentication,
                                                      @PathVariable String campusId,
                                                      @RequestBody CampusReview request) {
        guard.requireOperator(authentication.getName());
        return Envelope.ok(CampusView.from(service.reviewCampus(campusId, request.decision())));
    }
}
