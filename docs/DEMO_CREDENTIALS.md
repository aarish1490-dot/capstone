# Demo Credentials & Data

This document lists the pre-seeded accounts and demo data shipped in `db/seed.sql`. They exist so a reviewer can explore every part of the marketplace (browse, cart, checkout, orders, seller fulfilment, ratings) without registering. All passwords are real bcrypt hashes and all demo mobile numbers accept a mock/dev OTP when signing in with a phone (the code is shown in the UI/dev logs).

## Accounts

| Role | Email | Password | Mobile (OTP sign-in) |
|---|---|---|---|
| Platform Admin | `admin@aarishmart.com` | `Admin@123` | `9876500001` |
| Buyer | `buyer@aarishmart.com` | `Buyer@123` | `9844090001` |
| Student Buyer | `student@aarishmart.com` | `Student@123` | `9844090002` |
| Seller | `seller@aarishmart.com` | `Seller@123` | `9844090003` |
| Campus Seller | `campus@aarishmart.com` | `Campus@123` | `9844090004` |

Signing in with the email + password above works directly. Signing in with the mobile number sends a one-time OTP (in `development` OTP mode the code is printed to the UI/logs).

## What each account opens

- **Platform Admin** — admin dashboard: manages all users, products and orders, sees platform-wide stats.
- **Buyer** — home/catalog, and a **cart already contains 3 items** (Kalamkari Tote x1, Wooden Laptop Stand x1, Moon Mug x2) so checkout can be exercised instantly. Also has a full order history (see below).
- **Student Buyer** — a delivered order with a posted review on it.
- **Seller (Electronics)** — seller dashboard owns 4 catalog products (Wooden Laptop Stand, USB-C Multi-Port Hub, Foldable Phone Stand, Ceramic Moon Mug) and sees demo orders in all four statuses (see below).
- **Campus Seller (Books)** — seller dashboard owns 2 catalog products (Build Your First Cloud Project, Pocket Guide to Git & GitHub) and sees the delivered demo orders containing them.

## Demo order history

Showcased in the buyer's order history page and the two seller dashboards:

| Order id | Buyer | Status | Date | Contents | Total |
|---|---|---|---|---|---|
| 1001 | Demo Buyer | DELIVERED | 2026-08-28 | Tote, Wooden Laptop Stand, Git & GitHub Guide | 1727.00 |
| 1002 | Demo Buyer | SHIPPED | 2026-09-06 | USB-C Multi-Port Hub | 899.00 |
| 1003 | Demo Buyer | CONFIRMED | 2026-09-14 | Ceramic Moon Mug x2, Study Light | 1297.00 |
| 1004 | Demo Buyer | PENDING | 2026-09-19 | Foldable Phone Stand | 299.00 |
| 1005 | Student Buyer | DELIVERED | 2026-09-01 | Build Your First Cloud Project | 499.00 |

This means the Demo Seller can advance every status flag (PENDING → CONFIRMED → SHIPPED → DELIVERED) and both sellers have delivered orders with history.

## Demo reviews

| Product | Order | Reviewer | Rating |
|---|---|---|---|
| Wooden Laptop Stand | 1001 | Demo Buyer | 5 |
| Pocket Guide to Git & GitHub | 1001 | Demo Buyer | 4 |
| Build Your First Cloud Project | 1005 | Student Buyer | 4 |

Reviews only exist for DELIVERED orders, so the average-rating stars appear on those product pages.

## Notes

- These accounts and orders are **demo/private data only** — not intended for shipped production use.
- The 40-product catalog is unchanged; only ownership of 6 products was handed to the demo sellers so their dashboards are populated.
- New registrations, carts, orders and reviews use the seeded AUTO_INCREMENT sequences (next ids: 100+) and never collide with the demo data.