defmodule LoggingWeb.Router do
  use LoggingWeb, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/api", LoggingWeb do
    pipe_through :api
  end
  
  scope "/logging", LoggingWeb do
    pipe_through :api
    post "/entry", LoggingController, :post
  end
end
