defmodule RhWeb.RhController do
  use RhWeb, :controller
  alias HTTPoison.Response

  def get(conn, %{"uid" => uid}) do
    render(conn, "index.json", %{:uid => uid})
  end
  
  def expand_ous(conn, %{"uid" => uid}) do
    visit(conn, "https://almascience.eso.org/rh/ous/expand/#{uid}/downwards")
  end

  def expand_spw(conn, %{"uid" => uid}) do
    visit(conn, "https://2023aug.asa-test.hq.eso.org/rh/spw/expand/#{uid}/downwards")
  end
  
  defp visit(conn, url) do
    IO.puts("Visiting #{url}")
    
    # 15 second timeout should (hopefully) be plenty.
    {:ok, response} = HTTPoison.get(url, [], [recv_timeout: 25000])

    headers = response.headers
              |> Enum.into(%{})

    handle_follow_redirects(conn, headers, response.body)
  end
  
  defp handle_follow_redirects(conn, %{"Location" => location}, _body) do
    visit(conn, location)
  end
  
  defp handle_follow_redirects(conn, headers, body) do
    render(conn, "downwards.json", %{body: body})
  end
end
