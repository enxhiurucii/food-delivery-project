import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../../lib/http";
import { money } from "../../lib/format";
import type { ShopRestaurant } from "../../lib/types";
import { Badge, ErrorBanner, Loading } from "../../components/ui";
import { IconClock, IconHeadset, IconShield, IconStar, IconTruck } from "../../components/icons";
import { useShop } from "../WebshopApp";

const HERO_IMG =
  "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=80";

const CUISINES = [
  { key: "italian", title: "Italian", subtitle: "Pasta & pizza", className: "cuisine-italian", emoji: "🍝" },
  { key: "chinese", title: "Chinese", subtitle: "Wok favorites", className: "cuisine-chinese", emoji: "🥡" },
  { key: "japanese", title: "Japanese", subtitle: "Sushi & ramen", className: "cuisine-japanese", emoji: "🍣" },
  { key: "mexican", title: "Mexican", subtitle: "Tacos & more", className: "cuisine-mexican", emoji: "🌮" },
] as const;

export function RestaurantList() {
  const { api, searchQuery } = useShop();
  const [restaurants, setRestaurants] = useState<ShopRestaurant[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.restaurants().then(setRestaurants).catch((e) => setError(errorMessage(e)));
  }, [api]);

  const filtered = useMemo(() => {
    if (!restaurants) return null;
    const q = searchQuery.trim().toLowerCase();
    if (!q) return restaurants;
    return restaurants.filter(
      (r) => r.name.toLowerCase().includes(q) || (r.description ?? "").toLowerCase().includes(q),
    );
  }, [restaurants, searchQuery]);

  const firstOpen = filtered?.find((r) => r.open) ?? filtered?.[0];

  return (
    <section>
      <div className="hero-banner">
        <div className="hero-copy">
          <h1>Delicious Food Delivered Fast</h1>
          <p>Order from Tirana’s best kitchens for pickup or delivery — fresh, fast, and right to your door.</p>
          <div className="hero-actions">
            {firstOpen ? (
              <Link to={`/webshop/restaurants/${firstOpen.id}`} className="btn btn-primary btn-lg">
                Order Now
              </Link>
            ) : (
              <a href="#restaurants" className="btn btn-primary btn-lg">
                Order Now
              </a>
            )}
            <a href="#restaurants" className="btn btn-outline btn-lg">
              Explore Menu
            </a>
          </div>
          <div className="hero-social">
            <div className="avatar-stack" aria-hidden>
              <span>A</span>
              <span>M</span>
              <span>L</span>
              <span>+</span>
            </div>
            50K+ Happy Customers
          </div>
        </div>
        <div className="hero-visual">
          <img src={HERO_IMG} alt="Burger and fries" />
        </div>
      </div>

      <div className="feature-bar" id="features">
        <div className="feature-item">
          <div className="feature-icon"><IconTruck /></div>
          <div>
            <strong>Free Delivery</strong>
            <span>On selected partners</span>
          </div>
        </div>
        <div className="feature-item">
          <div className="feature-icon"><IconShield /></div>
          <div>
            <strong>Easy Returns</strong>
            <span>Hassle-free support</span>
          </div>
        </div>
        <div className="feature-item">
          <div className="feature-icon"><IconClock /></div>
          <div>
            <strong>Fast Prep</strong>
            <span>Live kitchen updates</span>
          </div>
        </div>
        <div className="feature-item">
          <div className="feature-icon"><IconHeadset /></div>
          <div>
            <strong>24/7 Support</strong>
            <span>We’re here to help</span>
          </div>
        </div>
      </div>

      <div className="section-head">
        <div>
          <h2>Explore by Cuisine</h2>
          <p>Browse moods — then pick a restaurant below.</p>
        </div>
      </div>
      <div className="cuisine-grid">
        {CUISINES.map((c) => (
          <a key={c.key} href="#restaurants" className={`cuisine-card ${c.className}`}>
            <div>
              <strong>{c.title}</strong>
              <span>{c.subtitle}</span>
            </div>
            <span className="cuisine-emoji" aria-hidden>{c.emoji}</span>
          </a>
        ))}
      </div>

      <div className="section-head" id="restaurants">
        <div>
          <h2>Popular Restaurants</h2>
          <p>{searchQuery.trim() ? `Results for “${searchQuery.trim()}”` : "Open kitchens ready to cook for you."}</p>
        </div>
      </div>

      <ErrorBanner message={error} />
      {!filtered && !error && (
        <Loading variant="page" label="Finding restaurants…" hint="Loading kitchens near you" />
      )}
      {filtered && filtered.length === 0 && (
        <div className="empty">No restaurants match your search.</div>
      )}
      <div className="restaurant-grid">
        {filtered?.map((r, index) => (
          <Link key={r.id} to={`/webshop/restaurants/${r.id}`} className={`restaurant-card product-card ${r.open ? "" : "closed"}`}>
            <div className="product-card-media">
              {index % 3 === 0 && r.open && <span className="product-badge"><Badge tone="green">Bestseller</Badge></span>}
              {index % 3 === 1 && r.open && <span className="product-badge"><Badge tone="amber">Popular</Badge></span>}
              {r.imageUrl ? <img src={r.imageUrl} alt="" /> : <div className="img-placeholder tall">🍽️</div>}
            </div>
            <div className="restaurant-card-body product-card-body">
              <div className="row space-between">
                <h3>{r.name}</h3>
                <Badge tone={r.open ? "green" : "red"}>{r.open ? "Open" : "Closed"}</Badge>
              </div>
              {r.description && <p className="muted small">{r.description}</p>}
              <div className="product-card-foot">
                <div>
                  <strong className="price">{money(r.minimumOrder)}+</strong>
                  <div className="small muted">
                    {r.estimatedPrepMinutes} min
                    {r.deliveryAvailable ? ` · Delivery ${money(r.deliveryFee)}` : " · Pickup"}
                  </div>
                </div>
                <span className="rating"><IconStar /> 4.{7 - (index % 3)}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>

      <div className="promo-grid">
        <div className="promo-card promo-mint">
          <div>
            <h3>Weekend Special</h3>
            <p>Free delivery on partner restaurants this weekend.</p>
            <a href="#restaurants" className="btn btn-primary btn-sm">Grab deal</a>
          </div>
          <div className="promo-visual" aria-hidden>🥗</div>
        </div>
        <div className="promo-card promo-peach">
          <div>
            <h3>Family Feast</h3>
            <p>Bigger portions for sharing — perfect for movie night.</p>
            <a href="#restaurants" className="btn btn-primary btn-sm">Browse menus</a>
          </div>
          <div className="promo-visual" aria-hidden>🍔</div>
        </div>
      </div>

      <div className="partners" id="partners">
        <span>Zomato</span>
        <span>Swiggy</span>
        <span>Uber Eats</span>
        <span>DoorDash</span>
        <span>Grubhub</span>
      </div>
    </section>
  );
}
