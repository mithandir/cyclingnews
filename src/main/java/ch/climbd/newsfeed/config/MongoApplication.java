package ch.climbd.newsfeed.config;

import ch.climbd.newsfeed.data.NewsEntry;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.ZonedDateTime;

@EnableMongoRepositories
public class MongoApplication
        extends AbstractMongoClientConfiguration {

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create();
    }

    @Override
    protected String getDatabaseName() {
        return "reactive";
    }

    @Override
    protected void configureConverters(MongoCustomConversions.MongoConverterConfigurationAdapter adapter) {
        adapter.registerConverter(new NewsEntryMongoReadConverter());
    }

    @ReadingConverter
    public class NewsEntryMongoReadConverter implements Converter<Document, NewsEntry> {

        public NewsEntry convert(Document document) {
            NewsEntry newsEntry = new NewsEntry();
            newsEntry.setLink(document.getString("link"));
            newsEntry.setTitle(document.getString("title"));
            newsEntry.setContent(document.getString("content"));
            newsEntry.setSummary(document.getString("summary"));
            newsEntry.setPublishedAt(document.getDate("publishedAt").toInstant().atZone(ZonedDateTime.now().getZone()));
            newsEntry.setVotes(document.getInteger("votes", 0));
            newsEntry.setViews(document.getInteger("views", 0));
            newsEntry.setLanguage(document.getString("language"));
            return newsEntry;
        }
    }
}
