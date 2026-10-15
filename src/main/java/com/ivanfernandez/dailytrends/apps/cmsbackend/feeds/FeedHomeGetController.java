package com.ivanfernandez.dailytrends.apps.cmsbackend.feeds;

import com.ivanfernandez.dailytrends.contexts.cms.feeds.application.home.FrontPageSearcher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedHomeGetController {

    private final FrontPageSearcher frontPageSearcher;

    public FeedHomeGetController(FrontPageSearcher frontPageSearcher) {
        this.frontPageSearcher = frontPageSearcher;
    }

    @GetMapping(path = "/feed/home", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "The front page: the 10 newest El Mundo and El Español feeds")
    @ApiResponse(
            responseCode = "200",
            description = "The stored feeds, newest first; a stale page also starts a background refresh",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = FeedResponse.class))))
    public List<FeedResponse> home() {
        return frontPageSearcher.search().stream()
                .map(feed -> FeedResponse.from(feed.toPrimitives()))
                .toList();
    }
}
