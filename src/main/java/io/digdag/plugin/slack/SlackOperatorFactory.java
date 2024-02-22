package io.digdag.plugin.slack;

import org.apache.http.HttpStatus;
import io.digdag.client.config.Config;
import io.digdag.client.config.ConfigException;
import io.digdag.spi.Operator;
import io.digdag.spi.OperatorContext;
import io.digdag.spi.OperatorFactory;
import io.digdag.spi.TaskResult;
import io.digdag.spi.TemplateEngine;
import io.digdag.util.BaseOperator;
import io.digdag.util.UserSecretTemplate;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SlackOperatorFactory
        implements OperatorFactory
{
    private final TemplateEngine templateEngine;

    private static final OkHttpClient singletonInstance = new OkHttpClient.Builder()
        .addInterceptor(new RetryInterceptor())
        .build();

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

            if (!params.has("webhook_url")) {
                throw new ConfigException("'webhook_url' is required");
            }
            String webhook_url = UserSecretTemplate.of(params.get("webhook_url", String.class))
                    .format(context.getSecrets());

            String message = workspace.templateCommand(templateEngine, params, "message", UTF_8);
            String payload = SlackPayload.convertToJson(message);

            this.postToSlack(webhook_url, payload);

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
                    String message = "status: " + response.code() + ", message: " + response.body().string();
                    throw new IOException(message);
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to send to Slack. " + e.getMessage(), e);
            }
            finally {
                singletonInstance.connectionPool().evictAll();
            }
        }
    }

    static class RetryInterceptor implements Interceptor
    {
        private static final int MAX_RETRY_COUNT = 5;
        private static final int SC_SLACK_RATE_LIMIT_OVER = 429;
        private static final Integer[] retryHttpStatus = {
            SC_SLACK_RATE_LIMIT_OVER,
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            HttpStatus.SC_BAD_GATEWAY,
            HttpStatus.SC_SERVICE_UNAVAILABLE
        };

        @Override
        public Response intercept(Chain chain) throws IOException
        {
            Request request = chain.request();
            // Response response = chain.proceed(request);
            Response response = new Response.Builder().code(503).message("503 Service Unavailable").body(ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), "{}")).protocol(okhttp3.Protocol.HTTP_1_1).request(request).build();
            int retryCount = 0;
            Logger logger = Logger.getLogger(RetryInterceptor.class.getName());
            while (!response.isSuccessful() && retryCount < MAX_RETRY_COUNT && Arrays.asList(retryHttpStatus).contains(response.code())) {
                retryCount++;
                logger.info("Retry count: " + retryCount);
    
                response.close();
                try {
                    Thread.sleep(getWaitTimeExponential(retryCount));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // retry request
                // response = chain.proceed(request);
                response = new Response.Builder().code(503).message("503 Service Unavailable").body(ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), "{}")).protocol(okhttp3.Protocol.HTTP_1_1).request(request).build();
            }
            return response;
        }

        public static long getWaitTimeExponential(int retryCount)
        {
            final long initialDelay = 1000L; // 1 second
            long waitTime = ((long) Math.pow(2, retryCount) * initialDelay);
            return waitTime;
        }
    }

}
