-- Name search ignores accents ("joao" finds "João"). unaccent ships with PostgreSQL contrib and Supabase.
CREATE EXTENSION IF NOT EXISTS unaccent;
