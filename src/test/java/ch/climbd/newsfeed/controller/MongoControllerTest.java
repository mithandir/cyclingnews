package ch.climbd.newsfeed.controller;

import ch.climbd.newsfeed.data.NewsEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class) // Necessary for @Mock and @InjectMocks if not using SpringRunner
public class MongoControllerTest {

    @Mock
    private MongoTemplate template;

    @InjectMocks
    private MongoController mongoController;

    @Test
    public void testFindAllOrderedByDate_Paginated() {
        int page = 0;
        int size = 10;
        Set<String> languages = new HashSet<>(Collections.singletonList("en"));

        // Prepare mock data
        List<NewsEntry> mockedNewsEntries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NewsEntry entry = new NewsEntry();
            entry.setTitle("Test News " + i);
            entry.setLink("http://example.com/news" + i);
            entry.setPublishedAt(ZonedDateTime.now().minusDays(i).toInstant());
            entry.setLanguage("en");
            mockedNewsEntries.add(entry);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));

        // Mocking template.count()
        // The query for count will not include pagination, but will include language and date criteria
        when(template.count(any(Query.class), eq(NewsEntry.class)))
                .thenReturn((long) mockedNewsEntries.size());

        // Mocking template.find()
        // The query for find will include pagination, language, and date criteria
        when(template.find(any(Query.class), eq(NewsEntry.class)))
                .thenReturn(mockedNewsEntries);

        // Call the method under test
        Page<NewsEntry> actualPage = mongoController.findAllOrderedByDate(languages, page, size);

        // Assertions
        assertNotNull(actualPage, "The returned page should not be null.");
        assertEquals(mockedNewsEntries.size(), actualPage.getContent().size(), "The content size should match the mock list size.");
        assertEquals(mockedNewsEntries.get(0).getTitle(), actualPage.getContent().get(0).getTitle(), "The first element's title should match.");
        assertEquals(mockedNewsEntries.size(), actualPage.getTotalElements(), "Total elements should match the mock list size.");
        assertEquals(page, actualPage.getPageable().getPageNumber(), "The page number should be correct.");
        assertEquals(size, actualPage.getPageable().getPageSize(), "The page size should be correct.");
        assertEquals(1, actualPage.getTotalPages(), "Total pages should be 1 for this test case.");

        // Verify mock interactions
        // Expect one call for count and one for find
        verify(template).count(any(Query.class), eq(NewsEntry.class));
        verify(template).find(any(Query.class), eq(NewsEntry.class));
    }

    @Test
    public void testFindAllOrderedByDate_EmptyResults() {
        int page = 0;
        int size = 10;
        Set<String> languages = new HashSet<>(Collections.singletonList("en"));

        List<NewsEntry> emptyList = Collections.emptyList();

        when(template.count(any(Query.class), eq(NewsEntry.class)))
                .thenReturn(0L);

        when(template.find(any(Query.class), eq(NewsEntry.class)))
                .thenReturn(emptyList);

        Page<NewsEntry> actualPage = mongoController.findAllOrderedByDate(languages, page, size);

        assertNotNull(actualPage);
        assertEquals(0, actualPage.getContent().size());
        assertEquals(0, actualPage.getTotalElements());
        assertEquals(page, actualPage.getPageable().getPageNumber());
        assertEquals(size, actualPage.getPageable().getPageSize()); // Pageable itself is still configured
        assertEquals(0, actualPage.getTotalPages());


        verify(template).count(any(Query.class), eq(NewsEntry.class));
        verify(template).find(any(Query.class), eq(NewsEntry.class));
    }

    @Test
    public void testFindAllOrderedByDate_MultiplePages() {
        int page = 1; // Second page
        int size = 5;
        long totalItems = 12; // e.g., 12 items in total
        Set<String> languages = Collections.singleton("de");

        List<NewsEntry> entriesForSecondPage = new ArrayList<>();
        for (int i = 0; i < Math.min(size, totalItems - (long)page * size) ; i++) { // Simulating items for the second page
            NewsEntry entry = new NewsEntry();
            entry.setTitle("German News " + (i + page * size));
            entry.setLink("http://beispiel.de/nachrichten" + (i + page * size));
            entry.setPublishedAt(ZonedDateTime.now().minusHours(i + page * size).toInstant());
            entry.setLanguage("de");
            entriesForSecondPage.add(entry);
        }

        when(template.count(any(Query.class), eq(NewsEntry.class)))
                .thenReturn(totalItems);
        when(template.find(any(Query.class), eq(NewsEntry.class)))
                .thenReturn(entriesForSecondPage);

        Page<NewsEntry> actualPage = mongoController.findAllOrderedByDate(languages, page, size);

        assertNotNull(actualPage);
        assertEquals(entriesForSecondPage.size(), actualPage.getContent().size());
        assertEquals(totalItems, actualPage.getTotalElements());
        assertEquals(page, actualPage.getPageable().getPageNumber());
        assertEquals(size, actualPage.getPageable().getPageSize());
        assertEquals((int) Math.ceil((double) totalItems / size), actualPage.getTotalPages());

        verify(template).count(any(Query.class), eq(NewsEntry.class));
        verify(template).find(any(Query.class), eq(NewsEntry.class));
    }
}
