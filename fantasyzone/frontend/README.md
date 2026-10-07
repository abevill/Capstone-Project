# FantasyZone Frontend

React 18 + TypeScript + Vite + React Router. Talks to the FantasyZone Spring Boot API.

## Run locally
    cp .env.example .env     # set VITE_API_URL to your backend
    npm install
    npm run dev              # http://localhost:5173
    npm run build            # type-check + production build into dist/

The backend must allow this origin: set CORS_ALLOWED_ORIGINS (default http://localhost:5173).

## Structure
- src/components: Button, Input, Select, Card, Modal, Table, Pagination, Feedback (Spinner/Skeleton/Error), Async, Navbar, ProtectedRoute
- src/context: AuthContext (useReducer + useMemo/useCallback), ThemeContext
- src/hooks/useFetch.ts: loading/error/reload for API calls
- src/services/api.ts: all API calls, JWT header, error handling
- src/pages: one file per route

## Routes
Public: / /login /register /leagues /leagues/:leagueId /players
Logged in: /dashboard /leagues/create /team/:teamId /team/:teamId/roster
Admin only: /admin

## Deploy to S3 (static hosting)
1. Set VITE_API_URL to the backend URL, then `npm run build`.
2. `aws s3 sync dist/ s3://<bucket> --delete`
3. Enable static website hosting with index.html as both the index and error document
   (React Router needs this so deep links like /leagues/3 work).
4. Set the backend CORS_ALLOWED_ORIGINS to the S3 website URL.
