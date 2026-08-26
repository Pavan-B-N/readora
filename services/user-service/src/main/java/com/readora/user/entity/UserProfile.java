package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * user_id is a cross-service reference to auth.User — plain UUID, never a JPA relationship,
 * since that entity lives in a different service/schema entirely.
 */
@Entity
@Table(name = "user_profiles", schema = "users")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone")
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "locale")
    private String locale = "en-IN";

    @Column(name = "marketing_opt_in", nullable = false)
    private boolean marketingOptIn = false;

    /** The store this customer shops from — quick-commerce delivers from one store at a time. */
    @Column(name = "preferred_store_id")
    private UUID preferredStoreId;

    /** Comma-separated category UUIDs, collected at signup to personalize recommendations — not a queried relational structure, same reasoning as Book.tableOfContents. */
    @Column(name = "favorite_category_ids", columnDefinition = "text")
    private String favoriteCategoryIds;

    protected UserProfile() {
    }

    public UserProfile(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    public void setMarketingOptIn(boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    public UUID getPreferredStoreId() {
        return preferredStoreId;
    }

    public void setPreferredStoreId(UUID preferredStoreId) {
        this.preferredStoreId = preferredStoreId;
    }

    public String getFavoriteCategoryIds() {
        return favoriteCategoryIds;
    }

    public void setFavoriteCategoryIds(String favoriteCategoryIds) {
        this.favoriteCategoryIds = favoriteCategoryIds;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserProfile that)) return false;
        return userId != null && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
