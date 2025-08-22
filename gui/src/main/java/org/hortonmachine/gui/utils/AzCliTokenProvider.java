package org.hortonmachine.gui.utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

public class AzCliTokenProvider {


    /**
     * Gets an AAD access token from the local Azure CLI for the given resource.
     * Requires: user has already run `az login` and has the right tenant/subscription selected.
     */
    public static String getAccessToken(String resource) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        if(resource == null){
            resource = "https://ossrdbms-aad.database.windows.net";
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("az");
        cmd.add("account");
        cmd.add("get-access-token");
        cmd.add("--resource");
        cmd.add(resource);
        cmd.add("--output");
        cmd.add("json");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Inherit PATH and other env vars (default). You can set pb.directory(...) if needed.

        Process process = pb.start();

        // Read stdout/stderr asynchronously to avoid deadlocks
        ExecutorService es = Executors.newFixedThreadPool(2);
        Future<String> outF = es.submit(() -> readAll(process.getInputStream()));
        Future<String> errF = es.submit(() -> readAll(process.getErrorStream()));

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            es.shutdownNow();
            throw new TimeoutException("Timed out running Azure CLI.");
        }

        String stdout = getFuture(outF);
        String stderr = getFuture(errF);
        es.shutdown();

        int exit = process.exitValue();
        if (exit != 0) {
            throw new IOException("Azure CLI failed (exit " + exit + "): " + stderr);
        }

        JSONObject responseJson = new JSONObject(stdout);
        String accessToken = responseJson.getString("accessToken");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("Could not find 'accessToken' in az output. Raw: " + stdout);
        }
        return accessToken;
    }

    private static String readAll(java.io.InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    private static String getFuture(Future<String> f) throws ExecutionException, InterruptedException, TimeoutException {
        return f.get(30, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        String resource = "https://ossrdbms-aad.database.windows.net";
        String token = getAccessToken(resource);
        System.out.println("Token: " + token);
    }
}