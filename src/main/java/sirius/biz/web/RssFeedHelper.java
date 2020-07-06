/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package sirius.biz.web;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import sirius.kernel.di.std.Register;
import sirius.kernel.xml.Outcall;
import sirius.web.security.HelperFactory;
import sirius.web.security.ScopeInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

/**
 * Helps to obtain an {@link SyndFeed} object
 */
public class RssFeedHelper {

    private static final String RSS_FEED_HELPER = "RSSFeedHelper";

    /**
     * Generates a helper instance per shop.
     */
    @Register
    public static class RssFeedHelperFactory implements HelperFactory<RssFeedHelper> {

        @Override
        public Class<RssFeedHelper> getHelperType() {
            return RssFeedHelper.class;
        }

        @Nonnull
        @Override
        public String getName() {
            return RSS_FEED_HELPER;
        }

        @Override
        public RssFeedHelper make(ScopeInfo scopeInfo) {
            return new RssFeedHelper();
        }
    }

    /**
     * Retrieve <tt>SyndFeed</tt> of the given URL
     *
     * @param feedUrl the URL to fetch the RSS feed from
     * @return a SyndFeed instance for the given URL
     * @throws IOException if feed can not be read by outcall
     * @throws FeedException if feed can not be parsed with rome parsers
     */
    public SyndFeed processFeed(String feedUrl) throws IOException, FeedException {
        Outcall outcall = new Outcall(new URL(feedUrl));
        return new SyndFeedInput().build(new StringReader(outcall.getData()));
    }
}
