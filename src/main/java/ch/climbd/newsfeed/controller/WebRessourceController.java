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

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n    <url>\n        <loc>https://news.qfotografie.de/</loc>\n        <lastmod>" + currentDate + "</lastmod>\n        <changefreq>daily</changefreq>\n        <priority>1.0</priority>\n    </url>\n    <url>\n        <loc>https://news.qfotografie.de/views</loc>\n        <lastmod>" + currentDate + "</lastmod>\n        <changefreq>daily</changefreq>\n        <priority>0.5</priority>\n    </url>\n    <url>\n        <loc>https://news.qfotografie.de/liked</loc>\n        <lastmod>" + currentDate + "</lastmod>\n        <changefreq>daily</changefreq>\n        <priority>0.5</priority>\n    </url>\n    <url>\n        <loc>https://news.qfotografie.de/seo</loc>\n        <lastmod>" + currentDate + "</lastmod>\n        <changefreq>daily</changefreq>\n        <priority>1.0</priority>\n    </url>\n    <url>\n        <loc>https://news.qfotografie.de/seo/views</loc>\n        <lastmod>" + currentDate + "</lastmod>\n        <changefreq>daily</changefreq>\n        <priority>0.5</priority>\n    </url>\n    <url>\n        <loc>https://news.qfotografie.de/seo/liked</loc>\n        <lastmod>" + currentDate + "</lastmod>\n        <changefreq>daily</changefreq>\n        <priority>0.5</priority>\n    </url>\n</urlset>\n";
    }
}
