package com.ivanfernandez.dailytrends.contexts.cms.feeds.infrastructure.scrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrontPageExtractorTest {

    private final FrontPageExtractor extractor = new FrontPageExtractor(".author", ".title", ".description");

    @Test
    void extracts_at_most_five_articles_in_document_order_counting_only_extracted_ones() {
        List<ScrapedArticle> articles = extractor.extract(HtmlFixtures.page("front-page-walk.html"));

        assertThat(articles)
                .extracting(article -> article.title())
                .containsExactly("Headline 1", "Headline 2", "Headline 3", "Headline 4", "Headline 5");
    }

    @Test
    void skips_articles_without_author_or_title_and_ignores_text_outside_articles() {
        List<ScrapedArticle> articles = extractor.extract(HtmlFixtures.page("front-page-walk.html"));

        assertThat(articles)
                .extracting(article -> article.title())
                .doesNotContain("No author element", "Blank author", "Promoted headline");
        assertThat(articles)
                .extracting(article -> article.author())
                .doesNotContain("Author 2", "Author 3", "Promo desk");
    }

    @Test
    void reads_the_author_title_and_description_of_each_article() {
        List<ScrapedArticle> articles = extractor.extract(HtmlFixtures.page("front-page-walk.html"));

        assertThat(articles.getFirst()).isEqualTo(new ScrapedArticle("Author 1", "Headline 1", "Description 1"));
    }

    @Test
    void joins_several_matches_with_a_space_and_collapses_whitespace() {
        List<ScrapedArticle> articles = extractor.extract(HtmlFixtures.page("front-page-walk.html"));

        assertThat(articles.get(1).author()).isEqualTo("Ana Luis");
        assertThat(articles.get(1).title()).isEqualTo("Headline 2");
    }

    @Test
    void a_missing_or_blank_description_is_null() {
        List<ScrapedArticle> articles = extractor.extract(HtmlFixtures.page("front-page-walk.html"));

        assertThat(articles.get(1).description()).as("missing").isNull();
        assertThat(articles.get(2).description()).as("blank").isNull();
    }

    @Test
    void finds_nothing_in_a_page_without_articles() {
        assertThat(extractor.extract(HtmlFixtures.page("no-articles.html"))).isEmpty();
    }
}
