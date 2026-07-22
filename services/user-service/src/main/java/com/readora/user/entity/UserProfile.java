package com.readora.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// user_id is a cross-service reference to auth.User — plain UUID, never a JPA relationship, since that entity lives in a different service/schema entirely.
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

    // The store an ADMIN is assigned to manage — deliberately separate from preferredStoreId and not exposed through UpdateProfileRequest, so an admin can never grant themselves another store's scope by editing their own profile; only set out-of-band (seed data today). Null for customers and unassigned admins.
    @Column(name = "admin_store_id")
    private UUID adminStoreId;

    /** Comma-separated category UUIDs, collected at signup to personalize recommendations — not a queried relational structure, same reasoning as Book.tableOfContents. */
    @Column(name = "favorite_category_ids", columnDefinition = "text")
    private String favoriteCategoryIds;

    // JPA no-arg constructor.
    protected UserProfile() {
    }

    // Creates a blank profile for a user.
    public UserProfile(UUID userId) {
        this.userId = userId;
    }

    // Profile owner's user id.
    public UUID getUserId() {
        return userId;
    }

    // Display name.
    public String getDisplayName() {
        return displayName;
    }

    // Sets the display name.
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // Avatar image URL.
    public String getAvatarUrl() {
        return avatarUrl;
    }

    // Sets the avatar image URL.
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    // Phone number.
    public String getPhone() {
        return phone;
    }

    // Sets the phone number.
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Date of birth.
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    // Sets the date of birth.
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // Locale.
    public String getLocale() {
        return locale;
    }

    // Sets the locale.
    public void setLocale(String locale) {
        this.locale = locale;
    }

    // Whether the user opted into marketing communications.
    public boolean isMarketingOptIn() {
        return marketingOptIn;
    }

    // Sets the marketing opt-in flag.
    public void setMarketingOptIn(boolean marketingOptIn) {
        this.marketingOptIn = marketingOptIn;
    }

    // Store this customer shops from.
    public UUID getPreferredStoreId() {
        return preferredStoreId;
    }

    // Sets the preferred (shopping) store.
    public void setPreferredStoreId(UUID preferredStoreId) {
        this.preferredStoreId = preferredStoreId;
    }

    // Store this admin manages, if any.
    public UUID getAdminStoreId() {
        return adminStoreId;
    }

    // Sets the store this admin manages.
    public void setAdminStoreId(UUID adminStoreId) {
        this.adminStoreId = adminStoreId;
    }

    // Comma-separated favorite category UUIDs.
    public String getFavoriteCategoryIds() {
        return favoriteCategoryIds;
    }

    // Sets the comma-separated favorite category UUIDs.
    public void setFavoriteCategoryIds(String favoriteCategoryIds) {
        this.favoriteCategoryIds = favoriteCategoryIds;
    }

    // Entity equality by user id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserProfile that)) return false;
        return userId != null && Objects.equals(userId, that.userId);
    }

    // Entity hash by user id.
    @Override
    public int hashCode() {
        return Objects.hashCode(userId);
    }
}
