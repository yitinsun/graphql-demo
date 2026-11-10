# Spring Boot GraphQL gateway: PostgreSQL + Contentful

One GraphQL schema over two data sources:

| Data | Source | Access |
|---|---|---|
| Products (price, stock, category) | PostgreSQL | Spring Data JPA + dynamic `Specification` filters |
| Articles, product marketing content | Contentful (Delivery/Preview REST API) | `RestClient`, cached 60s |
| Any other allowed content type | Contentful | generic `entries` query (dynamic filters / projection / order) |

## Run
```bash
docker compose up -d                       # PostgreSQL (Flyway creates + seeds the product table)
export CONTENTFUL_SPACE_ID=...             # optional: without it, products work and content fields return errors
export CONTENTFUL_TOKEN=...                # Content Delivery API access token
mvn spring-boot:run
```
GraphiQL: http://localhost:8080/graphiql   Schema: http://localhost:8080/graphql/schema

## Contentful content model to create
**article**: `title` (Short text), `slug` (Short text, unique), `body` (Long text), `publishedAt` (Date & time), `productSkus` (Short text, list)
**productContent**: `sku` (Short text), `title` (Short text), `description` (Long text), `image` (Media), `features` (Short text, list)

The two systems are linked by **SKU** (`AUD-001`, `AUD-002`, `AUD-003`, `ACC-001`, `ACC-002`, `WEA-001` are seeded in PostgreSQL), so create `productContent` entries with those SKUs and publish them.

## Example queries
Joined data, in one request:
```graphql
query Storefront {
  products(filter: { category: "audio", inStock: true }, sort: PRICE_ASC, size: 5) {
    totalElements
    items { sku name price stock content { title imageUrl features } }
  }
  articles(filter: { titleContains: "guide" }, limit: 3) {
    title slug
    relatedProducts { sku name price }
  }
}
```
Fully dynamic Contentful access:
```graphql
query Dynamic {
  entries(
    contentType: "article"
    filters: [{ field: "publishedAt", operator: GTE, value: "2026-01-01T00:00:00Z" }]
    select: ["title", "slug"]
    order: ["-sys.updatedAt"]
    limit: 5
  ) { total items { id fields } }
}
```
curl:
```bash
curl -s localhost:8080/graphql -H 'Content-Type: application/json' \
  -d '{"query":"{ products(size:3){ items { sku name price } } }"}'
```

## Design notes
- **No N+1**: `Product.content` and `Article.relatedProducts` use `@BatchMapping`, so a page of 20 products makes one Contentful call (`sku[in]`) and 20 articles make one SQL query.
- **Projection push-down**: `articles` asks Contentful only for the fields the client selected (`select=`), via `DataFetchingFieldSelectionSet`.
- **Safe dynamic queries**: Contentful field names are regex-validated, operators are an enum, values are sent as URI variables, and `entries` only serves content types in `contentful.allowed-content-types`.
- **Resilience**: timeouts on the HTTP client, 60s Caffeine cache, and a `@GraphQlExceptionHandler` so a Contentful outage nulls only the Contentful fields; PostgreSQL data is still returned (`errors[]` explains what failed).
- **Drafts**: set `CONTENTFUL_PREVIEW=true` with a Preview token for unpublished content (never in production).
- **Before production**: disable GraphiQL, add authentication, a max query depth/complexity limit, and cache invalidation via a Contentful webhook if 60s staleness is too long.
