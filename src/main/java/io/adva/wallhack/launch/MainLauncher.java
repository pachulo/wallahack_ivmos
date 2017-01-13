package io.adva.wallhack.launch;

import static org.kohsuke.args4j.ExampleMode.ALL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.adva.wallhack.models.Product;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by admin on 18/04/15.
 */
public class MainLauncher {

	private static final boolean DEBUG = false;

	@Option(name="-lat", usage="Latitude e.g. 41.398077")
	private String latitude = "41.398077";

	@Option(name="-long", usage="Longitude e.g. 2.170432")
	private String longitude = "2.170432";

	@Option(name="-cat", usage="Category e.g.\n" +
            "\t12345 (Electronic)\n" +
            "\t12463 (Books, Films & Music)\n" +
            "\t13100 (Electrical Applicances)")
	private String category = CAT_ELECTRONIC;

	@Option(name="-keyword", usage="Keyword e.g. tele")
	private String keyword = "tele";

	@Option(name="-min", usage="Min price e.g. 0")
	private Integer minPrice = 0;

	@Option(name="-max", usage="Max price e.g. 50")
	private Integer maxPrice = 50;

	@Option(name="-limit", usage="Limit items to show e.g. 20")
	private Integer limit = 20;

    @Option(name="-dist", usage="Distance in meters from location:\n" +
            "\t1000 (near)\n" +
            "\t5000 (zone)\n" +
            "\t10000 (city)\n" +
            "\t0 (no distance)")
    private Integer distance = DIST_CITY;

    @Option(name="-sort", usage="Criteria to sort the results e.g.\n" +"" +
            "\tcreationDate-des\n" +
            "\tsalePrice-asc")
    private String sort = SORT_DATE_DES;

	@Argument
	private List<String> arguments = new ArrayList<String>();

    static {
        System.setProperty("logback.configurationFile", "config/logging/logback.xml");
    }

    final static Logger LOG = LoggerFactory.getLogger(MainLauncher.class);

    public static final String BASE = "http://es.wallapop.com/search?";
    public static final String LAT_BCN = "41.398077";
    public static final String LONG_BCN = "2.170432";
    public static final String CAT_ELECTRONIC = "12545";
    public static final String SORT_DATE_DES = "creationDate-des";
    public static final Integer DIST_CITY = 10000;

    public static final String PRODUCT_BASE = "http://es.wallapop.com";

   public static void main (String[] args)
    {
        try {
            new MainLauncher().doMain(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doMain(String[] args) throws IOException {

        parseArgs(args);

        URL url = createUrl(keyword, minPrice, maxPrice, category, distance, sort, latitude, longitude);

        Document doc = null;
        try {
            doc = Jsoup.connect(url.toString()).get();
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        Elements containers = doc.getElementsByClass("container");
        Element container = containers.first();

        Elements cards = container.getElementsByClass("card");
        LOG.info("Scraping " + cards.size() + " products matching criteria : " +
                "category = " + category + ";" +
                minPrice + "<price<" + maxPrice + "; ");
        LOG.info(url.toString());


        ArrayList<Product> productList = new ArrayList<Product>();
        // Don't process more results than the limit
        for (int i=1; i <= cards.size() && i <= limit; i++) {
            Product p = parseProduct(cards.get(i));
            // If it's null it's because the product is reserved or sold
            if (p != null) {
                productList.add(p);
                LOG.trace(p.toString());
            }
        }

        int maxItems = limit ;
        LOG.info("Filtering latest "+maxItems+" items out of "+productList.size()+" retrieved:");

        printElems(productList, maxItems);
    }

    private void parseArgs(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);

        try {
            parser.parseArgument(args);

            if (args.length == 0) {
                throw new CmdLineException(parser, "No argument is given");
            }

        } catch (CmdLineException e ) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java MainLauncher [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            // print option sample. This is useful some time
            System.err.println("  Example: java SampleMain"+parser.printExample(ALL));

            if (!DEBUG) {
                System.exit(-1);
            }
        }
    }

    public static void printElems(ArrayList<Product> list,int max)
    {
        int i = 0;
        while (i < max && i < list.size()) {
            LOG.info(i+" : "+ list.get(i).toString());
            i++;
        }
    }

    public static Product parseProduct(Element card)
    {
        Product toRet = null;
        //Get price
        String priceString = getTextFromElement(card, "product-info-price")
                .replace("€", "")
                .replace("$", "")
                .replace("£", "");
        double price = Double.parseDouble(priceString);

        String title = getTextFromElement(card, "product-info-title");

        String category = getTextFromElement(card, "product-info-category");

        Element el = card.getElementsByClass("product-info-title").first();
        String linkHref = PRODUCT_BASE+el.attr("href");

        String description = getTextFromPage(linkHref, "card-product-detail-description");
        String location = getTextFromPage(linkHref, "card-product-detail-location");

        Element el1 = card.getElementsByClass("card-product-image").first();
        String imgURL =el1.attr("src");

        Element sellerHref =(card.getElementsByAttribute("href").get(3));
        String sellerURL = PRODUCT_BASE+sellerHref.attr("href");

        // Don't show reserved or sold items
        // TODO: Check if promoted items also have an icon
        if (card.getElementsByClass("status-icon").size() == 0) {
            toRet = new Product(title, description, linkHref, price, category, location, imgURL, sellerURL);
        }

        return toRet;
    }


    private static String getTextFromPage(String url, String clazz)
    {
        String toRet = "";
        Document doc = null;
        try {
            doc = Jsoup.connect(url.toString()).get();
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        Element target = doc.getElementsByClass(clazz).first();
        if (target == null) {
            return "";
        }
        return target.text();
    }

    public static String getTextFromElement(Element el, String clazz)
    {
        String toReturn = "";
        Element target = el.getElementsByClass(clazz).first();
        return  target.text();
    }


    public static URL createUrl(String keyword, int minPrice, int maxPrice, String category, int distance,
                                String sort, String latitude, String longitude){
        String minPriceStr ="";
        String maxPriceStr ="";
        keyword = keyword.replaceAll(" ","%20");
        if(minPrice!=0)
            minPriceStr=""+minPrice;
        if(maxPrice!=0)
            maxPriceStr=""+maxPrice;

        String urlString = BASE; //http://es.wallapop.com/search?
        urlString+="kws="+keyword+"&";
        urlString+="lat="+latitude+"&";
        urlString+="lng="+longitude+"&";
        if(category!="") {
            urlString += "catIds=" + category + "&";
        }
        urlString+="minPrice="+minPriceStr+"&";
        urlString+="maxPrice="+maxPriceStr+"&";
        if (distance != 0) {
            urlString+="dist=0_"+distance+"&";
        }
        else{
            urlString+="dist=0_&";
        }
        urlString+="order="+sort;
        URL toRet = null;
        try {
            toRet = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return toRet;

    }

}
