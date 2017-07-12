package io.digdag.plugin.slack;

import com.google.common.base.Throwables;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.digdag.client.config.Config;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.OperatorFactory;
import io.digdag.spi.TaskResult;
import io.digdag.spi.TemplateEngine;
import io.digdag.util.BaseOperator;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SlackOperatorFactory
        implements OperatorFactory
{
    private final TemplateEngine templateEngine;

    public SlackOperatorFactory(TemplateEngine templateEngine)
    {
        this.templateEngine = templateEngine;
    }

    public String getType()
    {
        return "slack";
    }

    @Override
    public Operator newOperator(OperatorContext context)
    {
        return new SlackOperator(context);
    }

    private class SlackOperator
            extends BaseOperator
    {
        public SlackOperator(OperatorContext context)
        {
            super(context);
        }

        @Override
        public TaskResult runTask()
        {
            Config params = request.getConfig().mergeDefault(
                    request.getConfig().getNestedOrGetEmpty("slack"));

            String message = workspace.templateCommand(templateEngine, params, "message", UTF_8);
            String url = params.get("webhook_url", String.class);
            String payload = SlackPayload.convertToJson(message);

            try {
                HttpResponse<String> res = Unirest.post(url)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .field("payload", payload).asString();
            } catch (UnirestException e) {
                e.printStackTrace();
            } finally {
                try {
                    Unirest.shutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return TaskResult.empty(request);
        }
    }
}
