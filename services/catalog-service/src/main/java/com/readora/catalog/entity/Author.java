package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

// JPA entity for a book author.
@Entity
@Table(name = "authors", schema = "catalog")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    // No-arg constructor required by JPA.
    protected Author() {
    }

    // Creates a new author with the given details.
    public Author(String name, String slug, String bio, String photoUrl) {
        this.name = name;
        this.slug = slug;
        this.bio = bio;
        this.photoUrl = photoUrl;
    }

    // Overwrites this author's mutable fields in place.
    public void update(String name, String slug, String bio, String photoUrl) {
        this.name = name;
        this.slug = slug;
        this.bio = bio;
        this.photoUrl = photoUrl;
    }

    // Returns the author's id.
    public UUID getId() {
        return id;
    }

    // Returns the author's name.
    public String getName() {
        return name;
    }

    // Returns the author's URL slug.
    public String getSlug() {
        return slug;
    }

    // Returns the author's bio text.
    public String getBio() {
        return bio;
    }

    // Returns the author's photo URL.
    public String getPhotoUrl() {
        return photoUrl;
    }

    // Entities are equal when they share a non-null id.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Author author)) return false;
        return id != null && Objects.equals(id, author.id);
    }

    // Hashes by id, consistent with equals.
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
