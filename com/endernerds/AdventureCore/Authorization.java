package com.endernerds.AdventureCore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

public class Authorization {
   private static final String CONFIG_DIR = "plugins/AuthSystem/Configs/";
   private static final String TOKEN_FILE = "plugins/AuthSystem/Configs/tokens.txt";
   private static final String SERVER_CONFIG_FILE = "plugins/AuthSystem/Configs/server.yml";
   private static final long REQUEST_COOLDOWN = 5000L;
   private static final long CONNECTION_LOG_COOLDOWN = 1800000L;
   private static final long FALLBACK_LOG_COOLDOWN = 3600000L;
   private static long lastConnectionLogTime = 0L;
   private static long lastFallbackLogTime = 0L;
   private static long lastCallTimestamp = 0L;
   private static String authResult = null;
   private static String token = loadToken();
   private static boolean VERBOSE_LOGGING = false;
   private static final List<Authorization.AuthServer> SERVERS = new ArrayList<>();

   public static void logVerbose(String msg) {
      if (VERBOSE_LOGGING) {
         Bukkit.getLogger().info("[AdventureCore][VERBOSE] " + msg);
      }
   }

   public static void toggleVerboseLogging() {
      VERBOSE_LOGGING = !VERBOSE_LOGGING;
      System.out.println("[AdventureCore] Verbose logging is now " + (VERBOSE_LOGGING ? "ENABLED" : "DISABLED"));
   }

   public static boolean isVerboseLogging() {
      return VERBOSE_LOGGING;
   }

   public static void loadServerConfig() {
      File configFile = new File("plugins/AuthSystem/Configs/server.yml");
      SERVERS.clear();
      if (!configFile.exists()) {
         SERVERS.add(new Authorization.AuthServer("169.155.122.114", 2321));
         SERVERS.add(new Authorization.AuthServer("50.20.250.60", 9151));
         SERVERS.add(new Authorization.AuthServer("bot.host2play.com", 2032));
         Bukkit.getLogger().warning("[Adventure Core] server.yml not found. Using default server list:");

         for (int i = 0; i < SERVERS.size(); i++) {
            Bukkit.getLogger().info("  " + (i == 0 ? "Primary" : "Fallback " + i) + ": " + SERVERS.get(i));
         }
      } else {
         try (InputStream input = new FileInputStream(configFile)) {
            Map<String, Object> config = (Map<String, Object>)new Yaml().load(input);
            if (config.containsKey("servers") && config.get("servers") instanceof List) {
               for (Object obj : (List)config.get("servers")) {
                  String entry = obj == null ? "" : obj.toString().trim();
                  if (!entry.contains(":")) {
                     Bukkit.getLogger().warning("[Auth] ERROR! Invalid server entry (missing ':'): " + entry);
                  } else {
                     String[] parts = entry.split(":");
                     if (parts.length != 2) {
                        Bukkit.getLogger().warning("[Auth] ERROR! Invalid server entry: " + entry);
                     } else {
                        String ip = parts[0].trim();

                        int port;
                        try {
                           port = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException var12) {
                           Bukkit.getLogger().warning("[Auth] ERROR! Invalid port for entry: " + entry);
                           continue;
                        }

                        SERVERS.add(new Authorization.AuthServer(ip, port));
                     }
                  }
               }
            }

            if (SERVERS.isEmpty()) {
               String ip = String.valueOf(config.getOrDefault("ip", "127.0.0.1"));
               int port = Integer.parseInt(config.getOrDefault("port", "8080").toString());
               SERVERS.add(new Authorization.AuthServer(ip, port));
            }

            Bukkit.getLogger().info("[Adventure Core] Config loaded: servers = " + SERVERS.size());

            for (int i = 0; i < SERVERS.size(); i++) {
               Bukkit.getLogger().info("  " + (i == 0 ? "Primary" : "Fallback " + i) + ": " + SERVERS.get(i));
            }

            logVerbose("Loaded server config: " + SERVERS);
         } catch (IOException var14) {
            logVerbose("IOException while loading server config: " + var14.getMessage());
            var14.printStackTrace();
         }
      }
   }

   private static JavaPlugin getPlugin() {
      return JavaPlugin.getProvidingPlugin(Authorization.class);
   }

   public static String handleAuthCode(String code) {
      long now = System.currentTimeMillis();
      boolean cacheable = authResult != null
         && !"connection_error".equals(authResult)
         && !"authorization_failure".equals(authResult)
         && !"invalid_code".equals(authResult);
      if (now - lastCallTimestamp < 5000L && cacheable) {
         return authResult;
      } else {
         lastCallTimestamp = now;
         handleAuthCodeAsync(code);
         return "Processing...";
      }
   }

   public static void handleAuthCodeAsync(String code) {
      if (code != null && !code.trim().isEmpty()) {
         Bukkit.getScheduler()
            .runTaskAsynchronously(
               getPlugin(),
               () -> {
                  try {
                     String response = sendCodeToDiscordWithFallback(code);

                     String result = switch (response) {
                        case "connection_error" -> "connection_error";
                        case "false" -> "authorization_failure";
                        case "Error" -> "error";
                        default -> response.startsWith("rate_limited") ? response : processAuthResponse(response);
                     };
                     setAuthResult(result);
                     if (result.startsWith("rate_limited")) {
                        Bukkit.getScheduler()
                           .runTask(
                              getPlugin(),
                              () -> Bukkit.getConsoleSender()
                                 .sendMessage("[Adventure Core] Your authorization attempt was throttled. You are trying too often!")
                           );
                     }

                     logVerbose("handleAuthCodeAsync: result = " + result);
                  } catch (Exception var5) {
                     Bukkit.getLogger().severe("[AdventureCore] ERROR: " + var5.getClass().getSimpleName() + ": " + var5.getMessage());
                     if (isVerboseLogging()) {
                        var5.printStackTrace();
                     }
                  }
               }
            );
      } else {
         setAuthResult("invalid_code");
         logVerbose("handleAuthCodeAsync called with empty code.");
      }
   }

   public static String sendCodeToDiscordWithFallback(String code) {
      long now = System.currentTimeMillis();
      String response = null;
      logVerbose("sendCodeToDiscordWithFallback: code = " + code);

      for (int i = 0; i < SERVERS.size(); i++) {
         Authorization.AuthServer srv = SERVERS.get(i);
         logVerbose("Trying server: " + srv);
         response = sendCodeToDiscord(code, srv.ip, srv.port);
         if (!"connection_error".equals(response)) {
            if (i > 0 && now - lastFallbackLogTime > 3600000L) {
               Bukkit.getLogger().warning("[Adventure Core] Primary servers unreachable, used fallback at " + srv.ip + ":" + srv.port);
               lastFallbackLogTime = now;
            }

            return response;
         }

         logVerbose("Server failed: " + srv + ", response = " + response);
      }

      logVerbose("All servers failed for code: " + code);
      return response;
   }

   public static String sendCodeToDiscord(String code, String serverIp, int port) {
      HttpURLConnection connection = null;

      String var12;
      try {
         logVerbose("Sending request to " + serverIp + ":" + port + " for code/token: " + (token != null ? "TOKEN" : "LICENSE"));
         JsonObject json = new JsonObject();
         String key = token != null ? "token" : "license";
         json.addProperty(key, token != null ? token : code);
         URL url = new URL("http://" + serverIp + ":" + port + "/sendValue");
         connection = (HttpURLConnection)url.openConnection();
         connection.setConnectTimeout(2000);
         connection.setReadTimeout(2000);
         connection.setRequestMethod("POST");
         connection.setRequestProperty("Content-Type", "application/json");
         connection.setDoOutput(true);

         try (OutputStream os = connection.getOutputStream()) {
            os.write(json.toString().getBytes());
         }

         int responseCode = connection.getResponseCode();
         logVerbose("Response code from " + serverIp + ":" + port + " = " + responseCode);
         InputStream stream = responseCode < 400 ? connection.getInputStream() : connection.getErrorStream();
         if (responseCode != 429) {
            String backendResp = new Scanner(stream).useDelimiter("\\A").next();
            logVerbose("Backend response: " + backendResp);
            return backendResp;
         }

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            JsonObject jsonResp = JsonParser.parseString(reader.lines().reduce("", (a, b) -> a + b)).getAsJsonObject();
            int retryAfter = jsonResp.has("retry_after") ? jsonResp.get("retry_after").getAsInt() : -1;
            logVerbose("Received 429 (Too Many Requests) from " + serverIp + ":" + port);
            var12 = retryAfter > 0 ? "rate_limited_" + retryAfter : "rate_limited";
         }
      } catch (ConnectException var28) {
         logVerbose("[ConnectException] " + serverIp + ":" + port + " -> " + var28.getMessage());
         logConnectionError("[Auth] Connection refused to " + serverIp + ":" + port + " -> " + var28.getMessage());
         return "connection_error";
      } catch (SocketTimeoutException var29) {
         logVerbose("[SocketTimeoutException] " + serverIp + ":" + port + " -> " + var29.getMessage());
         logConnectionError("[Auth] Timeout connecting to " + serverIp + ":" + port + " -> " + var29.getMessage());
         return "connection_error";
      } catch (UnknownHostException var30) {
         logVerbose("[UnknownHostException] " + serverIp + ":" + port + " -> " + var30.getMessage());
         logConnectionError("[Auth] Unknown host " + serverIp + ":" + port + " -> " + var30.getMessage());
         return "connection_error";
      } catch (IOException var31) {
         logVerbose("[IOException] " + serverIp + ":" + port + " -> " + var31.getMessage());
         logConnectionError("[Auth] IO error with " + serverIp + ":" + port + " -> " + var31.getMessage());
         if (VERBOSE_LOGGING) {
            var31.printStackTrace();
         }

         return "connection_error";
      } catch (Exception var32) {
         logVerbose("[Exception] Unexpected exception: " + var32);
         logConnectionError("[Auth] Unexpected exception:");
         if (VERBOSE_LOGGING) {
            var32.printStackTrace();
         }

         return "Error";
      } finally {
         if (connection != null) {
            connection.disconnect();
         }
      }

      return var12;
   }

   private static void logConnectionError(String msg) {
      long now = System.currentTimeMillis();
      if (now - lastConnectionLogTime > 1800000L) {
         Bukkit.getLogger().warning("[Adventure Core] " + msg);
         lastConnectionLogTime = now;
      }

      logVerbose("logConnectionError: " + msg);
   }

   private static String processAuthResponse(String response) {
      try {
         String trimmed = response.trim();
         if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            JsonObject json = JsonParser.parseString(trimmed).getAsJsonObject();
            if (json.has("error")) {
               String errorMsg = json.get("error").getAsString();
               String errorCode = json.has("code") ? json.get("code").getAsString() : "unknown_error";
               if (VERBOSE_LOGGING) {
                  Bukkit.getLogger().warning("[AdventureCore] Authorization error: " + errorMsg + " (code: " + errorCode + ")");
               } else {
                  Bukkit.getLogger().warning("[AdventureCore] Authorization error: " + errorMsg);
               }

               return errorCode;
            } else {
               String salt = json.has("salt") ? json.get("salt").getAsString() : null;
               String t = json.has("token") ? json.get("token").getAsString() : null;
               if (salt != null && t != null) {
                  saveToken(t);
                  return salt + ":" + t;
               } else {
                  Bukkit.getLogger().warning("[AdventureCore] Invalid response fields: salt or token missing.");
                  return "invalid_response_fields";
               }
            }
         } else {
            Bukkit.getLogger().warning("[AdventureCore] Invalid JSON format: " + trimmed);
            return "invalid_response_format";
         }
      } catch (Exception var5) {
         Bukkit.getLogger().warning("[AdventureCore] Failed to parse JSON response: " + response);
         if (VERBOSE_LOGGING) {
            var5.printStackTrace();
         }

         return "error_parsing_response";
      }
   }

   private static void setAuthResult(String result) {
      authResult = result;
      if ("invalid_code".equals(result) && VERBOSE_LOGGING) {
         Bukkit.getScheduler().runTask(getPlugin(), () -> Bukkit.getConsoleSender().sendMessage("Invalid code."));
      }

      logVerbose("setAuthResult: " + result);
   }

   private static void saveToken(String newToken) {
      try {
         Files.createDirectories(Paths.get("plugins/AuthSystem/Configs/"));
         Files.writeString(Paths.get("plugins/AuthSystem/Configs/tokens.txt"), newToken);
         token = newToken;
         logVerbose("Saved new token: " + newToken);
      } catch (IOException var2) {
         logVerbose("IOException while saving token: " + var2.getMessage());
         if (VERBOSE_LOGGING) {
            var2.printStackTrace();
         }
      }
   }

   private static String loadToken() {
      try {
         Path tokenPath = Paths.get("plugins/AuthSystem/Configs/tokens.txt");
         String loaded = Files.exists(tokenPath) ? Files.readString(tokenPath).trim() : null;
         logVerbose("Loaded token from file: " + loaded);
         return loaded;
      } catch (IOException var2) {
         logVerbose("IOException while loading token: " + var2.getMessage());
         if (VERBOSE_LOGGING) {
            var2.printStackTrace();
         }

         return null;
      }
   }

   public static void resetToken() {
      try {
         Files.deleteIfExists(Paths.get("plugins/AuthSystem/Configs/tokens.txt"));
         token = null;
         logVerbose("Token reset.");
      } catch (IOException var1) {
         logVerbose("IOException while resetting token: " + var1.getMessage());
         if (VERBOSE_LOGGING) {
            var1.printStackTrace();
         }
      }
   }

   static {
      loadServerConfig();
   }

   private static class AuthServer {
      String ip;
      int port;

      AuthServer(String ip, int port) {
         this.ip = ip;
         this.port = port;
      }

      @Override
      public String toString() {
         return this.ip + ":" + this.port;
      }
   }
}
