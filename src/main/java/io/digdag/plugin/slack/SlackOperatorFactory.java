package io.digdag.plugin.slack;

import io.digdag.client.config.Config;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.OperatorFactory;
import io.digdag.spi.TaskResult;
import io.digdag.spi.TemplateEngine;
import io.digdag.util.BaseOperator;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SlackOperatorFactory
        implements OperatorFactory
{
    private final TemplateEngine templateEngine;

    private static final OkHttpClient singletonInstance = new OkHttpClient();

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

            this.postToSlack(url, payload);

            return TaskResult.empty(request);
        }

        private void postToSlack(String url, String payload)
        {
            RequestBody body = new FormBody.Builder()
                    .add("payload", payload)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            Call call = singletonInstance.newCall(request);

            try (Response response = call.execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("posting to slack failed");
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                singletonInstance.connectionPool().evictAll();
            }
        }
    }
}
