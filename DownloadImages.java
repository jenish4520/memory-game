import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class DownloadImages {
    public static void main(String[] args) throws Exception {
        String baseDir = "src/main/resources/images";
        Files.createDirectories(Paths.get(baseDir));
        
        // Download Logo
        System.out.println("Downloading logo...");
        download("https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/International_Pok%C3%A9mon_logo.svg/320px-International_Pok%C3%A9mon_logo.svg.png", baseDir + "/logo.png");
        
        // IDs for Pokemon mapping to the names in GameLogic
        int[] ids = {25, 6, 1, 7, 39, 133, 143, 150, 94, 448, 151, 175, 54, 52, 68, 65, 130, 131, 132, 149, 59, 37, 38, 197, 196, 700, 470, 471, 135, 136, 134, 172, 26, 4, 5, 2};
        String[] names = {
            "Pikachu", "Charizard", "Bulbasaur", "Squirtle",
            "Jigglypuff", "Eevee", "Snorlax", "Mewtwo",
            "Gengar", "Lucario", "Mew", "Togepi",
            "Psyduck", "Meowth", "Machamp", "Alakazam",
            "Gyarados", "Lapras", "Ditto", "Dragonite",
            "Arcanine", "Vulpix", "Ninetales", "Umbreon",
            "Espeon", "Sylveon", "Leafeon", "Glaceon",
            "Jolteon", "Flareon", "Vaporeon", "Pichu",
            "Raichu", "Charmander", "Charmeleon", "Ivysaur"
        };
        
        for (int i = 0; i < ids.length; i++) {
            String url = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/" + ids[i] + ".png";
            download(url, baseDir + "/" + names[i] + ".png");
            System.out.println("Downloaded " + names[i]);
        }
        System.out.println("All images downloaded successfully!");
    }
    
    static void download(String urlStr, String path) {
        try {
            if (Files.exists(Paths.get(path))) return;
            // Add user agent
            java.net.URLConnection conn = new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("Failed to download: " + urlStr);
            e.printStackTrace();
        }
    }
}
