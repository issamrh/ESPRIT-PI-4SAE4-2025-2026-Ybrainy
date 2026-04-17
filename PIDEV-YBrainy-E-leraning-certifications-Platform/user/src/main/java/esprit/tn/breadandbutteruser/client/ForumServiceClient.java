package esprit.tn.breadandbutteruser.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "forum-service", contextId = "forumServiceClient")
public interface ForumServiceClient {

    @GetMapping("/api/forum/threads")
    List<Map<String, Object>> getThreads();
}
