package padej.soup.api.feature.module.setting;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import padej.soup.base.trait.Setupable;

public class SettingRepository implements Setupable {
   private final List<Setting> settings = Lists.newArrayList();

   @Override
   public void setup(Setting... setting) {
      this.settings.addAll(Arrays.asList(setting));
   }

   public Setting get(String name) {
      return this.settings.stream().filter(setting -> setting.getNameKey().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public List<Setting> settings() {
      return this.settings;
   }
}
