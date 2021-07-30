defmodule RhWeb.RhView do
  use RhWeb, :view

  def render("index.json", %{uid: uid}) do
    %{serverName: "data", path: "/data/" <> uid}
  end
end
