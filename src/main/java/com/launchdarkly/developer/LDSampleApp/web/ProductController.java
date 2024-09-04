package com.launchdarkly.developer.LDSampleApp.web;

import com.launchdarkly.developer.LDSampleApp.model.Product;
import com.launchdarkly.developer.LDSampleApp.model.ProductRepository;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.ImmutableStructure;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;
import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@RestController
@RequestMapping(path = "/api", produces = "application/json")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
class ProductController {

    @Getter
    @Setter
    private OpenFeatureAPI openFeatureAPI;

    @Getter
    @Setter
    private Supplier<ImmutableStructure> serverUserContext;

    @Getter
    @Setter
    private ProductRepository repository;

    @GetMapping(path = "/product/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable int id) {
        return this.repository.findById(id)
                .map(this::discountPrice)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Product discountPrice(final Product product) {
        var multiContext = new ImmutableContext(new HashMap<>() {
            {
                put("kind", new Value("multi"));
                put("user", new Value(serverUserContext.get()));
                put("product", new Value(new ImmutableStructure(new HashMap<>() {
                    {
                        put("key", new Value(String.valueOf(product.getId())));
                        put("type", new Value(product.getType()));
                        put("price", new Value(product.getPrice().doubleValue()));
                        put("name", new Value(product.getName()));
                    }
                })));
            }
        });

        final Client client = openFeatureAPI.getClient();
        final String discountPrincgFlag = "discount-pricing";
        var discountFlagValue = client.getDoubleValue(discountPrincgFlag, (double) 0, multiContext);
        var discountPercent = new BigDecimal(discountFlagValue);
        var discountAmount = product.getPrice().multiply(discountPercent).setScale(2, RoundingMode.HALF_UP);
        var adjustedPrice = product.getPrice().subtract(discountAmount);
        product.setPrice(adjustedPrice);
        return product;
    }
}
