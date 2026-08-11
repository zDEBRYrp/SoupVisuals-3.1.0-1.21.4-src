package padej.soup.core.server.websocket;

public class PlayerInfo {
   private final int id;
   private final String name;
   private final String role;
   private final boolean official;

   public int getPlayerId() {
      return this.id;
   }

   public String getNickname() {
      return this.name;
   }

   public PlayerInfo(int id, String name, String role) {
      this(id, name, role, false);
   }

   public int getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public String getRole() {
      return this.role;
   }

   public boolean isOfficial() {
      return this.official;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof PlayerInfo other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (this.getId() != other.getId()) {
         return false;
      } else if (this.isOfficial() != other.isOfficial()) {
         return false;
      } else {
         Object this$name = this.getName();
         Object other$name = other.getName();
         if (this$name == null ? other$name == null : this$name.equals(other$name)) {
            Object this$role = this.getRole();
            Object other$role = other.getRole();
            return this$role == null ? other$role == null : this$role.equals(other$role);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof PlayerInfo;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      result = result * 59 + this.getId();
      result = result * 59 + (this.isOfficial() ? 79 : 97);
      Object $name = this.getName();
      result = result * 59 + ($name == null ? 43 : $name.hashCode());
      Object $role = this.getRole();
      return result * 59 + ($role == null ? 43 : $role.hashCode());
   }

   @Override
   public String toString() {
      return "PlayerInfo(id=" + this.getId() + ", name=" + this.getName() + ", role=" + this.getRole() + ", official=" + this.isOfficial() + ")";
   }

   public PlayerInfo(int id, String name, String role, boolean official) {
      this.id = id;
      this.name = name;
      this.role = role;
      this.official = official;
   }
}
