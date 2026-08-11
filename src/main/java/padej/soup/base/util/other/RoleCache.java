package padej.soup.base.util.other;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import padej.soup.api.repository.client.OnlinePlayersManager;
import padej.soup.core.server.websocket.PlayerInfo;

public class RoleCache {
   private static final Map<String, String> roleCache = new HashMap<>();
   private static final Set<String> FALLBACK_DEVS = Set.of("Winvi_", "winvi", "Padej_");
   private static final Set<String> FALLBACK_YTS = Set.of("TheTrix", "felonn", "NazarioQp");
   private static final Set<String> FALLBACK_TESTERS = Set.of("nomadvorga", "Nomadvorga", "NomadvorgaYT", "Fix88");
   private static final Set<String> FALLBACK_PASTERS = Set.of("NightlyFever", "Nightly", "Anim4ik", "Flerni", "FlerniV1", "FlerniV2");
   private static final Set<String> FALLBACK_CROW = Set.of("IcyCrow");

   public static String getUserRole(String username) {
      PlayerInfo playerInfo = OnlinePlayersManager.getPlayerInfo(username);
      if (playerInfo != null) {
         String wsRole = convertWebSocketRole(playerInfo.getRole());
         roleCache.put(username, wsRole);
         return wsRole;
      }

      String cachedRole = roleCache.get(username);
      if (cachedRole != null) {
         return cachedRole;
      }

      String role = determineRoleFromFallback(username);
      roleCache.put(username, role);
      return role;
   }

   private static String convertWebSocketRole(String wsRole) {
      return switch (wsRole.toLowerCase()) {
         case "developers", "admin" -> "DEVELOPER";
         case "testers", "moderator", "mod" -> "TESTER";
         case "youtubers", "vip" -> "YOUTUBE";
         case "crow", "premium" -> "CROW";
         case "pasters", "paster" -> "PASTER";
         default -> "USER";
      };
   }

   private static String determineRoleFromFallback(String username) {
      if (containsIgnoreCase(FALLBACK_DEVS, username)) {
         return "DEVELOPER";
      } else if (containsIgnoreCase(FALLBACK_YTS, username)) {
         return "YOUTUBE";
      } else if (containsIgnoreCase(FALLBACK_TESTERS, username)) {
         return "TESTER";
      } else if (containsIgnoreCase(FALLBACK_PASTERS, username)) {
         return "PASTER";
      } else {
         return containsIgnoreCase(FALLBACK_CROW, username) ? "CROW" : "USER";
      }
   }

   private static boolean containsIgnoreCase(Set<String> set, String username) {
      return set.stream().anyMatch(s -> s.equalsIgnoreCase(username));
   }

   public static void clearCache() {
      roleCache.clear();
   }

   public static void shutdown() {
   }

   public static void forceRefresh() {
      clearCache();
   }

   public static Set<String> getDevelopers() {
      return FALLBACK_DEVS;
   }

   public static Set<String> getYoutubers() {
      return FALLBACK_YTS;
   }

   public static Set<String> getTesters() {
      return FALLBACK_TESTERS;
   }

   public static Set<String> getPasters() {
      return FALLBACK_PASTERS;
   }

   public static Set<String> getCrow() {
      return FALLBACK_CROW;
   }

   public static Map<String, String> getRoleCache() {
      return roleCache;
   }
}
