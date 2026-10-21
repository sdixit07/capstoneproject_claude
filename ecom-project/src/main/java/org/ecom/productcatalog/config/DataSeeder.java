//Use data seeder and command liner to insert data
package org.ecom.productcatalog.config;

import org.ecom.productcatalog.model.Category;
import org.ecom.productcatalog.Product;
import org.ecom.productcatalog.repository.CategoryRepository;
import org.ecom.productcatalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public void run(String... args) throws Exception {

        //Clear all existing data
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        //Create categories
        Category electronics = new Category();
        electronics.setName("Electronics");

        Category clothing = new Category();
        clothing.setName("Clothing");

        Category home= new Category();
        home.setName("Home and Kitchen");

        categoryRepository.saveAll(Arrays.asList(electronics,clothing,home));


        //Create products
        Product smartPhone =  new Product();
        smartPhone.setName("Smart phone");
        smartPhone.setDescription("Latest model samsung smart phone.");
        smartPhone.setImageUrl("https://placehold.co/600x400");
        smartPhone.setPrice(599.99);
        smartPhone.setCategory(electronics);

        Product laptop = new Product();
        laptop.setName("Laptop");
        laptop.setDescription("High performance laptop.");
        laptop.setImageUrl("https://placehold.co/600x400");
        laptop.setPrice(999.99);
        laptop.setCategory(electronics);

        Product jacket = new Product();
        jacket.setName("Winter jacket");
        jacket.setDescription("It gives warm feeling and light weight jacket.");
        jacket.setImageUrl("https://placehold.co/600x400");
        jacket.setPrice(99.99);
        jacket.setCategory(clothing);

        Product blender = new Product();
        blender.setName("Blender");
        blender.setDescription("High speed blender for smoothies and more.");
        blender.setImageUrl("https://placehold.co/600x400");
        blender.setPrice(129.99);
        blender.setCategory(home);

        List<Product> products = new ArrayList<>(Arrays.asList(smartPhone,laptop,jacket,blender));

        //Additional products so that pagination has more than one page
        String[][] extraElectronics = {
                {"Wireless headphones", "Noise cancelling over-ear headphones.", "149.99"},
                {"Smart watch", "Fitness tracking smart watch.", "199.99"},
                {"Tablet", "10 inch tablet with long battery life.", "329.99"},
                {"Bluetooth speaker", "Portable waterproof speaker.", "59.99"},
                {"Gaming console", "Next generation home gaming console.", "499.99"},
                {"Wireless mouse", "Ergonomic wireless mouse.", "24.99"},
                {"USB-C charger", "65W fast USB-C wall charger.", "34.99"}};
        String[][] extraClothing = {
                {"Running shoes", "Lightweight shoes for daily running.", "79.99"},
                {"Denim jeans", "Classic fit blue denim jeans.", "49.99"},
                {"Cotton t-shirt", "Soft breathable cotton t-shirt.", "19.99"},
                {"Wool sweater", "Warm knitted wool sweater.", "69.99"},
                {"Rain coat", "Waterproof hooded rain coat.", "89.99"},
                {"Leather belt", "Genuine leather belt.", "29.99"},
                {"Baseball cap", "Adjustable cotton cap.", "14.99"}};
        String[][] extraHome = {
                {"Coffee maker", "Programmable drip coffee maker.", "89.99"},
                {"Toaster", "Four slice stainless steel toaster.", "39.99"},
                {"Non-stick pan set", "Set of three non-stick frying pans.", "74.99"},
                {"Electric kettle", "Fast boiling 1.7 litre kettle.", "34.99"},
                {"Knife block set", "Eight piece stainless steel knife set.", "109.99"},
                {"Air fryer", "Digital air fryer with 5 litre basket.", "119.99"},
                {"Dinner set", "Sixteen piece ceramic dinner set.", "64.99"}};
        addProducts(products, extraElectronics, electronics);
        addProducts(products, extraClothing, clothing);
        addProducts(products, extraHome, home);

        productRepository.saveAll(products);
    }

    private void addProducts(List<Product> target, String[][] rows, Category category) {
        for (String[] row : rows) {
            Product p = new Product();
            p.setName(row[0]);
            p.setDescription(row[1]);
            p.setImageUrl("https://placehold.co/600x400");
            p.setPrice(Double.parseDouble(row[2]));
            p.setCategory(category);
            target.add(p);
        }
    }
}
