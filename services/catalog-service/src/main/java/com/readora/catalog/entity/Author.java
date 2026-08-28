package com.readora.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

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

    protected Author() {
    }

    public Author(String name, String slug, String bio, String photoUrl) {
        this.name = name;
        this.slug = slug;
        this.bio = bio;
        this.photoUrl = photoUrl;
    }

    public void update(String name, String slug, String bio, String photoUrl) {
        this.name = name;
        this.slug = slug;
        this.bio = bio;
        this.photoUrl = photoUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getBio() {
        return bio;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Author author)) return false;
        return id != null && Objects.equals(id, author.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
