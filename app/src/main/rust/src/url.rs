use anyhow::{Context, bail};

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum UrlKind {
    Ssh,
    Http,
    Https,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct UrlInfo {
    pub kind: UrlKind,
    pub path: String,
}

pub fn parse_url(url: &str) -> anyhow::Result<UrlInfo> {
    let str = bstr::BStr::new(url);

    let url = gix_url::parse(str)?;

    let kind = match &url.scheme {
        gix_url::Scheme::Ssh => UrlKind::Ssh,
        gix_url::Scheme::Http => UrlKind::Http,
        gix_url::Scheme::Https => UrlKind::Https,
        scheme => bail!("invalid scheme: {}", scheme),
    };

    Ok(UrlInfo {
        kind,
        path: url.path.to_string(),
    })
}

/// The same repository as a web address.
///
/// An ssh url names a host and a path but no scheme a browser can follow, so
/// opening one as it stands fails. Rebuilt as https it leads to the page the
/// repository is served from.
pub fn browser_url(url: &str) -> anyhow::Result<String> {
    let parsed = gix_url::parse(bstr::BStr::new(url))?;

    let host = parsed.host().context("url has no host")?;
    let path = parsed.path.to_string();
    let path = path.trim_start_matches('/').trim_end_matches(".git");

    Ok(format!("https://{host}/{path}"))
}

#[cfg(test)]
mod test {

    use super::*;

    #[test]

    fn test() {
        let url = parse_url("ssh://username@host:5555/dir/repo.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("https://github.com/wiiznokes/gitnote.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Https);

        let url = parse_url("git@github.com:wiiznokes/gitnote.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("git@git.sr.ht:~user/notes").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("ssh://name@9.9.9.9:111/name/name.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("name@git.dom.hu:111/name/name.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("ssh://name@git.dom.hu:111/name/name.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("name@git.dom.hu:/name/name.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);

        let url = parse_url("name@git.dom.hu:repos/name.git").unwrap();
        assert_eq!(&url.kind, &UrlKind::Ssh);
    }

    #[test]
    fn ssh_urls_become_web_addresses() {
        assert_eq!(
            browser_url("git@github.com:wiiznokes/gitnote.git").unwrap(),
            "https://github.com/wiiznokes/gitnote"
        );
        assert_eq!(
            browser_url("ssh://git@github.com:22/wiiznokes/gitnote.git").unwrap(),
            "https://github.com/wiiznokes/gitnote"
        );
        assert_eq!(
            browser_url("https://github.com/wiiznokes/gitnote.git").unwrap(),
            "https://github.com/wiiznokes/gitnote"
        );
    }
}
