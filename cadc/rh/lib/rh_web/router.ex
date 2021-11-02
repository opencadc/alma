defmodule RhWeb.Router do
  use RhWeb, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/api", RhWeb do
    pipe_through :api
  end

  scope "/rh", RhWeb do
    pipe_through :api
    get "/data/:uid/location", RhController, :get
  end
end
