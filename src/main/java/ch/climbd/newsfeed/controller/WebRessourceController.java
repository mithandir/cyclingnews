package ch.climbd.newsfeed.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class WebRessourceController {

    @RequestMapping(value = {"/robots.txt", "/robot.txt"}, method = RequestMethod.GET)
    @ResponseBody
    public String getRobotsTxt() {
        return """
                User-agent: *
                Disallow: /admin
                """;
    }

    @RequestMapping(value = {"/sitemap.xml"}, produces = {MediaType.APPLICATION_XML_VALUE}, method = RequestMethod.GET)
    @ResponseBody
    public String getSitemap() {

        var currentDate = LocalDateTime.now().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);

        return STR."""
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
            <url>
                <loc>https://news.qfotografie.de/latest</loc>
                <lastmod>\{ currentDate }</lastmod>
                <changefreq>daily</changefreq>
                <priority>1.0</priority>
            </url>
            <url>
                <loc>https://news.qfotografie.de/views</loc>
                <lastmod>\{ currentDate }</lastmod>
                <changefreq>daily</changefreq>
                <priority>0.5</priority>
            </url>
            <url>
                <loc>https://news.qfotografie.de/liked</loc>
                <lastmod>\{ currentDate }</lastmod>
                <changefreq>daily</changefreq>
                <priority>0.5</priority>
            </url>
        </urlset>
        """;
    }
}
