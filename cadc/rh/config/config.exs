# This file is responsible for configuring your application
# and its dependencies with the aid of the Mix.Config module.
#
# This configuration file is loaded before any dependency and
# is restricted to this project.

# General application configuration
use Mix.Config

# Configures the endpoint
config :rh, RhWeb.Endpoint,
  url: [host: "localhost"],
  secret_key_base: "X043+1QrUK87iuyIgidIm0i42SU/UDnQNOhxrZd2oMAcceZJjQ6gOw4hZAK3zSja",
  render_errors: [view: RhWeb.ErrorView, accepts: ~w(json), layout: false],
  pubsub_server: Rh.PubSub,
  live_view: [signing_salt: "ZJIP6Jzf"]

# Configures Elixir's Logger
config :logger, :console,
  format: "$time $metadata[$level] $message\n",
  metadata: [:request_id]

# Use Jason for JSON parsing in Phoenix
config :phoenix, :json_library, Jason

# Import environment specific config. This must remain at the bottom
# of this file so it overrides the configuration defined above.
import_config "#{Mix.env()}.exs"
