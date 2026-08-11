package padej.soup.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.api.repository.friend.Friend;
import padej.soup.api.repository.friend.FriendUtils;

public class FriendFile extends ClientFile {
   public FriendFile() {
      super("friends");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try (FileWriter writer = new FileWriter(file)) {
         gson.toJson(FriendUtils.getFriends(), writer);
      } catch (JsonIOException | IOException e) {
         throw new FileSaveException(String.format("Failed to save %s to file", this.getName()), e);
      }
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      Gson gson = new Gson();
      File file = new File(path, this.getName() + ".json");

      try (FileReader reader = new FileReader(file)) {
         Friend[] friends = (Friend[])gson.fromJson(reader, Friend[].class);
         FriendUtils.clear();
         FriendUtils.getFriends().addAll(Arrays.asList(friends));
      } catch (IOException e) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), e);
      } catch (JsonSyntaxException e) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), e);
      } catch (JsonIOException e) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), e);
      }
   }
}
