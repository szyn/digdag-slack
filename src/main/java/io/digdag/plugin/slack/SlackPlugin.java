package io.digdag.plugin.slack;

import io.digdag.spi.OperatorFactory;
import io.digdag.spi.OperatorProvider;
import io.digdag.spi.Plugin;
import io.digdag.spi.TemplateEngine;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;

public class SlackPlugin
        implements Plugin
{
    @Override
    public <T> Class<? extends T> getServiceProvider(Class<T> type)
    {
        if (type == OperatorProvider.class) {
            return SlackOperatorProvider.class.asSubclass(type);
        }
        else {
            return null;
        }
    }

    public static class SlackOperatorProvider
            implements OperatorProvider
    {
        @Inject
        protected TemplateEngine templateEngine;

        @Override
        public List<OperatorFactory> get()
        {
            return Arrays.asList(
                    new SlackOperatorFactory(templateEngine));
        }
    }
}
