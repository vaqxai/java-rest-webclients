
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.application.Platform;
import javafx.embed.swing.*;
import javafx.scene.Scene;

import javax.swing.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Country {

    @JsonProperty("name")
    public String name;

    @JsonProperty("iso3")
    public String iso3;

    @JsonProperty("iso2")
    public String iso2;

    @JsonProperty("currency")
    public String currency;

    @JsonProperty("cities")
    public City[] cities;
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class City {

    @JsonProperty("name")
    public String name;

    @JsonProperty("latitude")
    public double latitude;

    @JsonProperty("longitude")
    public double longitude;

}

class WeatherInfo {

    public String weather;
    public String description;
    public String temperature;

    @JsonProperty("weather")
    private void unpackWeatherInfo(List<Map<String, String>> weatherInfoPacked){
        weather = weatherInfoPacked.get(0).get("main");
        description = weatherInfoPacked.get(0).get("description");
    }

    @JsonProperty("main")
    private void unpackMainInfo(Map<String, String> mainInfoPacked){
        temperature = mainInfoPacked.get("temp");
    }

}

class ExchangeRate {
    
        public String currency;
        public String rate;
    
        @JsonProperty("rates")
        private void unpackExchangeRate(Map<String, String> exchangeRatePacked){
            currency = exchangeRatePacked.entrySet().iterator().next().getKey();
            rate = exchangeRatePacked.entrySet().iterator().next().getValue();
        }
    
}

class NPBExchangeRate {

    public String rate;

    @JsonProperty("currency")
    public String currency;

    @JsonProperty("rates")
    private void unpackExchangeRate(List<Map<String, String>> exchangeRatePacked){
        System.out.println(exchangeRatePacked);
        rate = exchangeRatePacked.get(0).get("mid");
    }

}

public class App {

    // App.getExchangeRate("USD", "PLN");
    // App.getWeatherDescription({23,52});
    // App.getNBPExchangeRate("USD");
    // Ewentualnie: App.getCityCoordinates("Warszawa");

    public static <T> T getObjectFromUrl(String url, Class<T> clazz) throws IOException {

        URL urlObj = null;

        try {
			urlObj = new URL(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        HttpURLConnection connection = null;
		connection = (HttpURLConnection) urlObj.openConnection();

        try {
			connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        int responseCode = connection.getResponseCode();
        System.out.println("URL RESPONSE: " + responseCode);

        if(responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader( connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            System.out.println(response.toString());
            T objInfo = objectMapper.readValue(response.toString(), clazz);

            return objInfo;
        } else {
            return null;
        }

    }

    public static double getExchangeRate(String currencyA, String currencyB) throws IOException {
        String url = "https://api.exchangerate.host/latest?base=" + currencyA + "&symbols=" + currencyB;
        ExchangeRate exchangeRate = getObjectFromUrl(url, ExchangeRate.class);
        System.out.println(exchangeRate.rate);
        return Double.parseDouble(exchangeRate.rate);
    }

    public static WeatherInfo getWeatherDescription(double[] coordinates) throws IOException{
        String weatherApiKey = "0ab93af4cec4624cb2f70208d3d9b711";
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + coordinates[0] + "&lon=" + coordinates[1] + "&appid=" + weatherApiKey + "&units=metric";

        return getObjectFromUrl(url, WeatherInfo.class);
    }

    public static double getNPBExchangeRate(String currencyFrom) throws IOException {
        String url = "https://api.nbp.pl/api/exchangerates/rates/A/" + currencyFrom + "/?format=json";
        NPBExchangeRate exchangeRate = getObjectFromUrl(url, NPBExchangeRate.class);
        if(exchangeRate == null){
            return 0.0;
        }
        return Double.parseDouble(exchangeRate.rate);
    }
    public static void main(String[] args) throws Exception {

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        JFrame frame = new JFrame("Enter your selection");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300,300);

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Country> countries = objectMapper.readValue(new File("countries+cities.json"), new TypeReference<List<Country>>(){});
        Map<String,String> currencies = objectMapper.readValue(new File("currencies.json"), new TypeReference<Map<String,String>>(){});

        JComboBox<String> countryComboBox = new JComboBox<>();

        for (Country country : countries) {
            countryComboBox.addItem(country.name);
        }

        JComboBox<String> cityComboBox = new JComboBox<>();

        JComboBox<String> exchangeRateFrom = new JComboBox<>();

        for (Map.Entry<String, String> entry : currencies.entrySet()) {
            exchangeRateFrom.addItem(entry.getKey() + " - " + entry.getValue());
        }

        JButton button = new JButton("Fetch");

        cityComboBox.addActionListener(l -> {
            String selectedCity = (String) cityComboBox.getSelectedItem();
            if (selectedCity == null) {
                button.setEnabled(false);
            } else {
                button.setEnabled(true);
            }
        });

        countryComboBox.addActionListener(e -> {
            String selectedCountry = (String) countryComboBox.getSelectedItem();
            List<String> cities = new ArrayList<>();
            for (Country country : countries) {
                if (country.name.equals(selectedCountry)) {
                    for (City city : country.cities) {
                        cities.add(city.name);
                    }
                }
            }
            cityComboBox.removeAllItems();
            for (String city : cities) {
                cityComboBox.addItem(city);
            }
        });

        countryComboBox.setSelectedIndex(0);

        button.addActionListener(l -> {
            double[] coord = {
                countries.get(countryComboBox.getSelectedIndex()).cities[cityComboBox.getSelectedIndex()].latitude,
                countries.get(countryComboBox.getSelectedIndex()).cities[cityComboBox.getSelectedIndex()].longitude
            };
            WeatherInfo weatherInfo = null;
            double exchangeRate = 0.0;
            double nbpExchangeRate = 0.0;
            try {
                weatherInfo = getWeatherDescription(coord);
                exchangeRate = getExchangeRate(((String)(exchangeRateFrom.getSelectedItem())).split(" - ")[0], countries.get(countryComboBox.getSelectedIndex()).currency);
                nbpExchangeRate = getNPBExchangeRate(countries.get(countryComboBox.getSelectedIndex()).currency);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return;
            }

            JFXPanel fxPanel = new JFXPanel();

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    WebView webView = new WebView();
                    Scene scene = new Scene(webView);

                    JFrame frame2 = new JFrame();
                    fxPanel.setScene(scene);
                    frame2.add(fxPanel);
                    frame2.setSize(800, 600);
                    frame2.setVisible(true);

                    WebEngine webEngine = webView.getEngine();
                    webEngine.load("https://en.wikipedia.org/wiki/" + cityComboBox.getSelectedItem());
                }
            });

            JOptionPane.showMessageDialog(frame,
            "Country: " + countryComboBox.getSelectedItem() +
            "\nCity: " + cityComboBox.getSelectedItem() +
            "\nWeather: " + weatherInfo.description +
            "\nTemperature: " + weatherInfo.temperature + "°C" +
            "\nExchange rate: 1 " + ((String)exchangeRateFrom.getSelectedItem()).split(" - ")[0] + " = " + exchangeRate + " " + countries.get(countryComboBox.getSelectedIndex()).currency+
            "\nNBP: 1 " + countries.get(countryComboBox.getSelectedIndex()).currency + " = " + nbpExchangeRate + " PLN");

        });

        JLabel countryLabel = new JLabel("Country");
        JLabel cityLabel = new JLabel("City");
        JLabel exchangeRateLabel = new JLabel("Exchange from");

        var l = new GroupLayout(frame.getContentPane());
        frame.getContentPane().setLayout(l);

        l.setAutoCreateGaps(true);
        l.setAutoCreateContainerGaps(true);

        l.setVerticalGroup(
            l.createSequentialGroup()
            .addGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER, false)
                .addComponent(countryLabel)
                .addComponent(countryComboBox)
            )
            .addGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER, false)
                .addComponent(cityLabel)
                .addComponent(cityComboBox)
            )
            .addGroup(l.createParallelGroup(GroupLayout.Alignment.CENTER, false)
                .addComponent(exchangeRateLabel)
                .addComponent(exchangeRateFrom)
            )
            .addComponent(button)
        );

        l.setHorizontalGroup(
            l.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
            .addGroup(
                l.createSequentialGroup()
                    .addGroup(
                        l.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                        .addComponent(countryLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cityLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(exchangeRateLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    )
                    .addGroup(
                        l.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                        .addComponent(countryComboBox)
                        .addComponent(cityComboBox)
                        .addComponent(exchangeRateFrom)
                    )
            )
            .addComponent(button)
        );

        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);

    }
}
