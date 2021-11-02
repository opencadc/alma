defmodule RhWeb.RhController do
  use RhWeb, :controller

  def get(conn, %{"uid" => uid}) do
    render(conn, "index.json", %{:uid => uid})
  end
end
