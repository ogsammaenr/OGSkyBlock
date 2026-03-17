package me.ogsammenr.skyblock.model;

public enum IslandRole {
    VISITOR(0),   // Ziyaretçi (Adada kaydı olmayan herkes)
    MEMBER(1),    // Üye (Tier 1)
    TRUSTED(2),   // Güvenilir (Tier 2)
    COOP(3),      // Ortak (Tier 3)
    OWNER(4);     // Ada Sahibi

    private final int weight;

    IslandRole(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    // Bir rolün, diğer bir rolden üstün veya ona eşit olup olmadığını kontrol eder
    public boolean isAtLeast(IslandRole requiredRole) {
        return this.weight >= requiredRole.getWeight();
    }
}
