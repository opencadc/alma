defmodule LoggingWeb.LoggingController do
  use LoggingWeb, :controller

  def post(conn, params) do
    IO.puts(Jason.encode!(params, [{:pretty, :true}]))
    json(conn, %{body: params})
  end
end
