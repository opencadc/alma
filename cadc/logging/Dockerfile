FROM elixir:slim

WORKDIR /app
ENV MIX_ENV=prod
ENV SECRET_KEY_BASE=xeLcKhT2W7ogsYWBZpyeJlcw76mYCbeQNvv8giGUKK0SOcotX0QrzJv+VM1E3kMV

COPY config ./config
COPY lib ./lib
COPY priv ./priv
COPY mix.exs .
#COPY mix.lock .

RUN mix local.hex --force \
    && mix local.rebar --force \
    && mix deps.get \
    && mix release \
    && mix phx.digest
