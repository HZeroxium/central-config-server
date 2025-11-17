package com.example.sample.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration Properties for demo visualization.
 * 
 * Supports dynamic configuration refresh via Spring Cloud Bus.
 * Properties are automatically rebound when refresh occurs.
 * 
 * Note: @ConfigurationProperties classes automatically rebind on refresh
 * without requiring @RefreshScope (which is needed for @Value injection).
 */
@ConfigurationProperties(prefix = "demo")
@Validated
public class DemoProperties {
    
    @Valid
    private Theme theme = new Theme();
    
    @Valid
    private Banner banner = new Banner();
    
    @Valid
    private Metrics metrics = new Metrics();
    
    @Valid
    private Status status = new Status();
    
    public DemoProperties() {}
    
    // Getters and setters
    public Theme getTheme() {
        return theme;
    }
    
    public void setTheme(Theme theme) {
        this.theme = theme != null ? theme : new Theme();
    }
    
    public Banner getBanner() {
        return banner;
    }
    
    public void setBanner(Banner banner) {
        this.banner = banner != null ? banner : new Banner();
    }
    
    public Metrics getMetrics() {
        return metrics;
    }
    
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics != null ? metrics : new Metrics();
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status != null ? status : new Status();
    }
    
    /**
     * Theme configuration for visual styling.
     */
    @Validated
    public static class Theme {
        @NotBlank
        private String primaryColor = "#4CAF50";
        
        @NotBlank
        private String secondaryColor = "#81C784";
        
        @NotBlank
        private String backgroundColor = "#E8F5E9";
        
        @NotBlank
        private String textColor = "#1B5E20";
        
        public Theme() {}
        
        public String getPrimaryColor() {
            return primaryColor;
        }
        
        public void setPrimaryColor(String primaryColor) {
            this.primaryColor = primaryColor != null ? primaryColor : "#4CAF50";
        }
        
        public String getSecondaryColor() {
            return secondaryColor;
        }
        
        public void setSecondaryColor(String secondaryColor) {
            this.secondaryColor = secondaryColor != null ? secondaryColor : "#81C784";
        }
        
        public String getBackgroundColor() {
            return backgroundColor;
        }
        
        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor != null ? backgroundColor : "#E8F5E9";
        }
        
        public String getTextColor() {
            return textColor;
        }
        
        public void setTextColor(String textColor) {
            this.textColor = textColor != null ? textColor : "#1B5E20";
        }
    }
    
    /**
     * Banner configuration for header display.
     */
    @Validated
    public static class Banner {
        @NotBlank
        private String text = "Sample Service";
        
        private boolean show = true;
        
        private String logoUrl = "";
        
        public Banner() {}
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text != null ? text : "Sample Service";
        }
        
        public boolean isShow() {
            return show;
        }
        
        public void setShow(boolean show) {
            this.show = show;
        }
        
        public String getLogoUrl() {
            return logoUrl;
        }
        
        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl != null ? logoUrl : "";
        }
    }
    
    /**
     * Metrics configuration for display.
     */
    @Validated
    public static class Metrics {
        @Min(0)
        private int retryCount = 3;
        
        @Min(0)
        private int cacheTtl = 300;
        
        public Metrics() {}
        
        public int getRetryCount() {
            return retryCount;
        }
        
        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount >= 0 ? retryCount : 3;
        }
        
        public int getCacheTtl() {
            return cacheTtl;
        }
        
        public void setCacheTtl(int cacheTtl) {
            this.cacheTtl = cacheTtl >= 0 ? cacheTtl : 300;
        }
    }
    
    /**
     * Status badge configuration.
     */
    @Validated
    public static class Status {
        @NotBlank
        private String badge = "DEFAULT";
        
        @NotBlank
        private String badgeColor = "#757575";
        
        public Status() {}
        
        public String getBadge() {
            return badge;
        }
        
        public void setBadge(String badge) {
            this.badge = badge != null ? badge : "DEFAULT";
        }
        
        public String getBadgeColor() {
            return badgeColor;
        }
        
        public void setBadgeColor(String badgeColor) {
            this.badgeColor = badgeColor != null ? badgeColor : "#757575";
        }
    }
}

