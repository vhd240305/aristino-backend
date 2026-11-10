package com.aristino.backend.service;

import com.aristino.backend.common.SlugUtil;
import com.aristino.backend.entity.Product;
import com.aristino.backend.entity.ProductVariant;
import com.aristino.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Danh muc san pham thu nho" cho chatbot: doc cac san pham DANG BAN tu database, luu tam 60 giay trong bo nho,
 * va chon ra cac san pham lien quan den cau hoi (tim theo tu khoa, khong phan biet dau) de dua vao loi nhac cho AI.
 * Day la cach lam don gian phu hop quy mo cua hang nho (vai tram san pham). Chi chua thong tin san pham, khong co du lieu ca nhan.
 */
@Service
@RequiredArgsConstructor
public class ChatCatalogService {

    private static final long TTL_MS = 60_000;
    private static final List<String> SIZE_ORDER = List.of("XS", "S", "M", "L", "XL", "XXL", "2XL", "3XL", "4XL", "5XL");

    // Cac tu pho bien khong giup phan biet san pham (da bo dau)
    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "minh", "ban", "muon", "can", "cho", "la", "co", "khong", "nao", "gi", "va", "cua", "de", "mot",
            "cac", "nhung", "the", "nay", "do", "voi", "hay", "xin", "vui", "long", "shop", "oi", "nhe", "nha",
            "duoc", "bi", "da", "se", "dang", "sao", "nhu", "ma", "thi", "tu", "den", "tai", "ve", "rat", "qua",
            "lam", "cung", "nua", "con", "nen", "hoi", "giup", "van", "cam", "on", "a", "o", "ah", "ad", "gium");

    private final ProductRepository productRepository;

    private volatile List<Item> cache = List.of();
    private volatile long loadedAt = 0;

    public record Item(Long id, String name, String slug, String category, BigDecimal minPrice, BigDecimal maxPrice,
                       String material, List<String> sizes, List<String> colors, int stock, Set<String> tokens) {
    }

    // ===== Xu ly tu khoa =====

    // Tach cau thanh cac tu khoa bo dau, bo tu pho bien. "\u00c1o s\u01a1 mi tr\u1eafng" -> [ao, so, mi, trang]
    public static Set<String> tokensOf(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (String word : rawWords(text)) {
            if (!STOP_WORDS.contains(word)) {
                out.add(word);
            }
        }
        return out;
    }

    // Tat ca cac tu (bo dau), khong loc tu pho bien
    public static List<String> rawWords(String text) {
        List<String> words = new ArrayList<>();
        for (String w : SlugUtil.slugify(text).split("-")) {
            if (!w.isBlank()) {
                words.add(w);
            }
        }
        return words;
    }

    // ===== Du lieu san pham =====

    public List<Item> items() {
        long now = System.currentTimeMillis();
        if (now - loadedAt > TTL_MS) {
            synchronized (this) {
                if (now - loadedAt > TTL_MS) {
                    cache = load();
                    loadedAt = System.currentTimeMillis();
                }
            }
        }
        return cache;
    }

    private List<Item> load() {
        List<Item> result = new ArrayList<>();
        for (Product p : productRepository.findAllWithVariantsByStatus(Product.Status.ACTIVE)) {
            List<ProductVariant> variants = new ArrayList<>(p.getVariants());
            List<ProductVariant> inStock = variants.stream().filter(v -> v.getStockQuantity() > 0).toList();

            BigDecimal min = variants.stream().map(ProductVariant::getPrice).min(Comparator.naturalOrder()).orElse(p.getBasePrice());
            BigDecimal max = variants.stream().map(ProductVariant::getPrice).max(Comparator.naturalOrder()).orElse(p.getBasePrice());

            Set<String> sizes = new LinkedHashSet<>();
            inStock.stream().map(ProductVariant::getSize).filter(s -> s != null && !s.isBlank())
                    .sorted(Comparator.comparingInt((String s) -> sizeRank(s)).thenComparing(Comparator.naturalOrder()))
                    .forEach(sizes::add);
            Set<String> colors = new LinkedHashSet<>();
            inStock.stream().map(ProductVariant::getColor).filter(c -> c != null && !c.isBlank()).forEach(colors::add);

            String category = p.getCategory().getName();
            String material = p.getMaterial() == null ? "" : p.getMaterial();
            result.add(new Item(
                    p.getId(), p.getName(), p.getSlug(), category, min, max, material,
                    new ArrayList<>(sizes), new ArrayList<>(colors),
                    variants.stream().mapToInt(ProductVariant::getStockQuantity).sum(),
                    tokensOf(p.getName() + " " + category + " " + material)));
        }
        return result;
    }

    private static int sizeRank(String size) {
        int index = SIZE_ORDER.indexOf(size.trim().toUpperCase());
        return index < 0 ? SIZE_ORDER.size() : index;
    }

    // San pham lien quan den cau hoi: cham diem theo so tu khoa trung; neu cau ngan thi tham khao them cau truoc do cua khach
    public List<Item> relevant(String message, Collection<String> recentUserTexts, int limit) {
        Set<String> wanted = tokensOf(message);
        if (wanted.size() < 2 && recentUserTexts != null) {
            for (String previous : recentUserTexts) {
                wanted.addAll(tokensOf(previous));
            }
        }
        if (wanted.isEmpty()) {
            return List.of();
        }

        Map<Item, Integer> scores = new LinkedHashMap<>();
        for (Item item : items()) {
            int score = 0;
            for (String token : item.tokens()) {
                if (wanted.contains(token)) {
                    score++;
                }
            }
            if (score > 0) {
                scores.put(item, score);
            }
        }
        // Diem cao truoc; bang diem thi san pham moi hon (id lon hon) truoc
        List<Map.Entry<Item, Integer>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> {
            int byScore = Integer.compare(b.getValue(), a.getValue());
            return byScore != 0 ? byScore : Long.compare(b.getKey().id(), a.getKey().id());
        });
        List<Item> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : sorted) {
            if (result.size() >= limit) {
                break;
            }
            result.add(entry.getKey());
        }
        return result;
    }

    // ===== Van ban du lieu dua cho AI =====

    public String contextFor(String message, Collection<String> recentUserTexts) {
        List<Item> all = items();
        StringBuilder sb = new StringBuilder();
        sb.append(ChatTexts.CONTEXT_HEADER).append('\n');
        sb.append(ChatTexts.STORE_INFO).append("\n\n");

        sb.append(ChatTexts.CATEGORIES).append('\n');
        Map<String, List<Item>> byCategory = new LinkedHashMap<>();
        for (Item item : all) {
            byCategory.computeIfAbsent(item.category(), k -> new ArrayList<>()).add(item);
        }
        byCategory.forEach((category, list) -> {
            BigDecimal min = list.stream().map(Item::minPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            BigDecimal max = list.stream().map(Item::maxPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            sb.append("- ").append(category).append(": ").append(list.size()).append(" s\u1ea3n ph\u1ea9m, gi\u00e1 t\u1eeb ")
                    .append(money(min)).append(" \u0111\u1ebfn ").append(money(max)).append('\n');
        });

        List<Item> related = relevant(message, recentUserTexts, 10);
        boolean featured = related.isEmpty();
        List<Item> shown = featured
                ? all.stream().sorted(Comparator.comparing(Item::id).reversed()).limit(8).toList()
                : related;

        sb.append('\n').append(featured ? ChatTexts.FEATURED_PRODUCTS : ChatTexts.RELATED_PRODUCTS).append('\n');
        if (shown.isEmpty()) {
            sb.append(ChatTexts.NO_PRODUCTS).append('\n');
        }
        for (Item item : shown) {
            sb.append("- ").append(describe(item)).append('\n');
        }
        return sb.toString();
    }

    public String describe(Item item) {
        StringBuilder sb = new StringBuilder(item.name());
        sb.append(" | Danh m\u1ee5c: ").append(item.category());
        sb.append(" | Gi\u00e1: ").append(money(item.minPrice()));
        if (item.maxPrice().compareTo(item.minPrice()) != 0) {
            sb.append(" - ").append(money(item.maxPrice()));
        }
        if (!item.material().isBlank()) {
            sb.append(" | Ch\u1ea5t li\u1ec7u: ").append(item.material());
        }
        if (item.stock() <= 0 || item.sizes().isEmpty()) {
            sb.append(" | T\u00ecnh tr\u1ea1ng: h\u1ebft h\u00e0ng");
        } else {
            sb.append(" | Size c\u00f2n h\u00e0ng: ").append(String.join(", ", item.sizes()));
            if (!item.colors().isEmpty()) {
                sb.append(" | M\u00e0u c\u00f2n h\u00e0ng: ").append(String.join(", ", item.colors()));
            }
        }
        sb.append(" | \u0110\u01b0\u1eddng d\u1eabn: /products/").append(item.slug());
        return sb.toString();
    }

    // 399000 -> "399.000\u0111"
    public static String money(BigDecimal value) {
        return String.format("%,d", value.longValue()).replace(',', '.') + "\u0111";
    }
}